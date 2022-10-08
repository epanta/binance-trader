package com.panta.cryptobot.repositories;

import com.panta.cryptobot.entities.TradeOrder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TradeOrderRepository extends JpaRepository<TradeOrder, Integer> {

    List<TradeOrder> findAllByFinishedIsFalseAndSimulatedIsFalse();

    Optional<TradeOrder> findFirstByPairAndFinishedIsFalseAndSimulatedIsFalse(String pair);

    Optional<TradeOrder> findFirstByPairAndFinishedIsTrueAndSimulatedIsFalseOrderByBuyDateDesc(String pair);

    Optional<TradeOrder> findFirstByPairAndFinishedIsTrueAndSimulatedIsTrueOrderByBuyDateDesc(String pair);

    List<TradeOrder> findAllByFinishedIsFalseAndSimulatedIsTrue();

    Optional<TradeOrder> findFirstByPairAndFinishedIsFalseAndSimulatedIsTrue(String pair);
}
