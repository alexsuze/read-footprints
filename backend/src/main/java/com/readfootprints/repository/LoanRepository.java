package com.readfootprints.repository;

import com.readfootprints.model.Loan;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Collection;
import java.util.List;

public interface LoanRepository extends MongoRepository<Loan, String> {

    List<Loan> findByActiveBookIdIn(Collection<String> bookIds);

    boolean existsByActiveBookId(String bookId);

    List<Loan> findAllByOrderByRequestedAtDesc();

    List<Loan> findByBorrowerIdOrderByRequestedAtDesc(String borrowerId);
}
