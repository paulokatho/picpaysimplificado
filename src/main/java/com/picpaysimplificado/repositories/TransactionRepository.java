package com.picpaysimplificado.repositories;

import com.picpaysimplificado.domain.Transaction.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
}
