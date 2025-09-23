package co.com.bancolombia.model.notifications;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class CapacityRequest{
        private Long applicationId;
        private String email;
        private String loanType;
        private double amount;
        private int loanTermMonths;
        private double annualInterestRate;
        private String customerId;
        private double maxIndebtedness;
        private double currentMonthlySalary;
        private int currentLoanMonthlyPayment;
        private int totalApprovedLoansMonthlyPayment;
}