package co.com.bancolombia.r2dbc.entities;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.OffsetDateTime;

@Table("loan_application")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class LoanApplicationEntity {

    @Id
    @Column("id")
    private Long loanApplicationId;

    @Column("user_id")
    private Long userId;

    @Column("document")
    private String document;

    @Column("email")
    private String email;

    @Column("loan_type")
    private String loanType;

    private BigInteger amount;

    @Column("term_months")
    private Integer loanTermMonths;

    private String status;

    @Column("interest_rate")
    private BigDecimal interestRate;

    @Column("total_monthly")
    private Integer totalMonthly;

    @Column("created_at")
    private OffsetDateTime createdAt;

    @Column("updated_at")
    private OffsetDateTime updatedAt;

    private String observations;
}
