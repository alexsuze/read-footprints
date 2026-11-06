package com.readfootprints.service;

import com.readfootprints.model.Loan;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LoanServiceTest {

    private static Loan loan(String status) {
        Loan l = new Loan();
        l.status = status;
        return l;
    }

    @Test
    void activeMeansRequestedOrLent() {
        assertTrue(loan(Loan.REQUESTED).active());
        assertTrue(loan(Loan.LENT).active());
        assertFalse(loan(Loan.RETURNED).active());
    }

    @Test
    void statusFilter() {
        assertTrue(LoanService.matches(loan(Loan.RETURNED), null));
        assertTrue(LoanService.matches(loan(Loan.RETURNED), ""));
        assertTrue(LoanService.matches(loan(Loan.LENT), "active"));
        assertFalse(LoanService.matches(loan(Loan.RETURNED), "active"));
        assertTrue(LoanService.matches(loan(Loan.REQUESTED), Loan.REQUESTED));
        assertFalse(LoanService.matches(loan(Loan.LENT), Loan.REQUESTED));
    }
}
