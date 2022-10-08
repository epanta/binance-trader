package com.panta.cryptobot.services.strategies.generic.impl;

import com.panta.cryptobot.dependency.domain.market.TickerStatistics;
import com.panta.cryptobot.entities.TradeOrder;
import com.panta.cryptobot.repositories.TradeOrderRepository;
import com.panta.cryptobot.services.strategies.generic.SellDefaultService;
import com.panta.cryptobot.util.TraderUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@RequiredArgsConstructor
@Slf4j
public class SellDefaultServiceImpl implements SellDefaultService {

    private final TradeOrderRepository tradeOrderRepository;

    @Value("${binance.buyer.order.currency}")
    private String BUYER_CURRENCY;

    @Value("${binance.seller.percentage.limit.loss}")
    private BigDecimal MAX_PERCENTAGE_LOSS;

    @Value("${binance.seller.percentage.limit.gain}")
    private BigDecimal MAX_PERCENTAGE_GAIN;

    @Value("${binance.seller.percentage.limit.hold}")
    private BigDecimal HOLD_PERCENTAGE;

    @Value("${binance.seller.percentage.limit.loss.related.gain}")
    private BigDecimal MAX_PERCENTAGE_LOSS_RELATED_GAIN;

    @Value("${binance.currency.percentage.limit}")
    private BigDecimal PERCENTAGE_LIMIT;

    @Value("${binance.seller.percentage.limit.keep.coin}")
    private Boolean KEEP_COIN;

    private final TraderUtil traderUtil;

    @Override
    public boolean keepInOperation(final TickerStatistics marketTickerResponse, TradeOrder order) {
        boolean result;
        Double initialPrice = Double.valueOf(order.getInitialPrice().doubleValue());

        setDefaultPercentageLimit(marketTickerResponse.getStrategy());

        BigDecimal profitPercentage = traderUtil.calculatePercentage(initialPrice,
        Double.valueOf(marketTickerResponse.getLastPrice()), false);

        result = conditionToFinishRelatedToInitialPrice(marketTickerResponse, order,
                profitPercentage);

        updateOrderData(order, initialPrice, Double.valueOf(marketTickerResponse.getLastPrice()), profitPercentage);

        return result;
    }

    private void setDefaultPercentageLimit(int strategy) {
        if (strategy == 6) {
            this.MAX_PERCENTAGE_GAIN = this.PERCENTAGE_LIMIT;
        }
    }

    private void updateOrderData(TradeOrder order, Double initialPrice, Double currentPrice, BigDecimal profitPercentage) {
        order.setProfitPercentage(profitPercentage.setScale(2, RoundingMode.DOWN));
        order.setUpdatedPrice(BigDecimal.valueOf(currentPrice));
        order.setIncreasedValue(new BigDecimal(currentPrice-initialPrice).setScale(2, RoundingMode.DOWN));

        Double currentProfit = order.getBuyCurrencyAmount().doubleValue() * currentPrice;
        order.setProfitValue(new BigDecimal(currentProfit - order.getBuyValue().doubleValue()).setScale(8, RoundingMode.DOWN));

        tradeOrderRepository.save(order);
    }



    private boolean conditionToFinishRelatedToInitialPrice(final TickerStatistics marketTickerResponse,
                                                           TradeOrder order, BigDecimal profitPercentage) {
        return conditionToFinish(marketTickerResponse, order, profitPercentage);
    }

    private boolean conditionToFinish(final TickerStatistics marketTickerResponse,
                                      TradeOrder order, BigDecimal profitPercentage){

        // Entra quando o valor percentual de ganho for positivo
        if (profitPercentage.compareTo(BigDecimal.ZERO) > 0) {
            return conditionToFinishRelatedToProfitPercentage(order, profitPercentage,
                    BigDecimal.valueOf(Double.valueOf(marketTickerResponse.getLastPrice())));
        }

        order.setMaxPercentage(MAX_PERCENTAGE_GAIN);

        //Entre se estiver perdendo mas nao chegou ao limite em relacao valor comprado
        if (profitPercentage.compareTo(MAX_PERCENTAGE_LOSS) > 0) {
            log.info("Percentual de perda em {}%. Permanecendo com o ativo {} [{}].",
                    profitPercentage.setScale(2, RoundingMode.DOWN), traderUtil.getSymbolFormatted(order.getPair(), BUYER_CURRENCY), order.getOperatorBuyId());
            return true;
        }

        // O valor percentual foi menor do que 6% em relacao ao valor inicial comprado, guarda a moeda
        if (profitPercentage.compareTo(HOLD_PERCENTAGE) <= 0) {
            log.info("Percentual de perda em {}%. Percentual muito negativo, holdando o ativo {} [{}].",
                    profitPercentage.setScale(4, RoundingMode.HALF_UP), traderUtil.getSymbolFormatted(order.getPair(), BUYER_CURRENCY), order.getOperatorBuyId());
            return true;
        }

        // Entre se e o percentual de perda for maior do que o aceitavel (perda grande)
        if (profitPercentage.compareTo(MAX_PERCENTAGE_LOSS) <= 0) {
            log.info("Percentual de perda em {}%. Percentual abaixo do limite, vendendo o ativo {}  [{}].",
            //log.info("Percentual de perda em {}%. Percentual abaixo do limite, holdando o ativo {}  [{}].",
                    profitPercentage.doubleValue(), traderUtil.getSymbolFormatted(order.getPair(), BUYER_CURRENCY), order.getOperatorBuyId());
            return KEEP_COIN;

        }

        log.info("Percentual em nenhuma das condicoes de venda ou compra. Percentual do  {} em {}",
                traderUtil.getSymbolFormatted(order.getPair(), BUYER_CURRENCY), profitPercentage);

        return true;
    }

    private boolean conditionToFinishRelatedToProfitPercentage(TradeOrder order, BigDecimal profitPercentage,
                                                               BigDecimal currentValue) {
        if (profitPercentage.compareTo(MAX_PERCENTAGE_GAIN) >= 0) { //Entre se chegou no ganho maximo em relacao ao valor comprado

            if (order.getMaxPercentage() == null) {
                order.setMaxPercentage(MAX_PERCENTAGE_GAIN);
            }

            BigDecimal total = BigDecimal.valueOf(profitPercentage.doubleValue() - order.getMaxPercentage().doubleValue());

            if (total.compareTo(MAX_PERCENTAGE_LOSS_RELATED_GAIN) < 0 ) {
                log.info("Percentual de perda = {}% em relacao ao lucro ja obtido = {}%, com percentual atual = {}. " +
                                "Percentual abaixo do limite, vendendo o ativo {} [{}].",
                        total, order.getMaxPercentage(), profitPercentage, traderUtil.getSymbolFormatted(order.getPair(), BUYER_CURRENCY), order.getOperatorBuyId());
                return false;
            } else {
                log.info("Perda em relacao ao lucro - Total ={}", total);
            }

            if (profitPercentage.compareTo(order.getMaxPercentage()) >= 0) {
                updateMaxPercentage(profitPercentage, order);
                log.info("Percentual de ganho atual = {}% >= max ja atingido = {}. Preco atual = {}, " +
                                "Preco anterior = {}, Percentual subindo... Continua com o ativo {} [{}].",
                        profitPercentage.doubleValue(), order.getMaxPercentage(), currentValue, order.getUpdatedPrice(),
                        traderUtil.getSymbolFormatted(order.getPair(), BUYER_CURRENCY), order.getOperatorBuyId());
            } else {
                log.info("Percentual de ganho atual = {}% < Max ja atingido = {}. Preco atual = {}, " +
                                "Preco anterior = {}, Percentual caindo dentro do limite... Continua com o ativo {} [{}].",
                        profitPercentage.doubleValue(), order.getMaxPercentage(), currentValue, order.getUpdatedPrice(),
                        traderUtil.getSymbolFormatted(order.getPair(), BUYER_CURRENCY), order.getOperatorBuyId());
            }
        }

        log.info("Percentual de ganho atual = {}% < max ja atingido = {}. Preco atual = {}, " +
                        "Preco anterior = {}, Percentual positivo... Continua com o ativo {} [{}].",
                profitPercentage.doubleValue(), order.getMaxPercentage(), currentValue, order.getUpdatedPrice(),
                traderUtil.getSymbolFormatted(order.getPair(), BUYER_CURRENCY), order.getOperatorBuyId());
        return true;
    }

    private void updateMaxPercentage(BigDecimal currentPercentage, TradeOrder order) {
        if (currentPercentage.compareTo(order.getMaxPercentage()) > 0) {
            order.setMaxPercentage(currentPercentage);
            log.info("ATUALIZANDO MAX PERCENTAGE DE {} do ID = {}", order.getPair(), order.getOperatorBuyId());
            tradeOrderRepository.save(order);
        }
    }

}