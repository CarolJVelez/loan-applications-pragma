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

@Table("loan_type")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class LoanTypeEntity {

    @Id
    @Column("id")
    private Long loanTypeId;
    private String code;
    private String name;
    private Boolean active;
    @Column("minimum_amount")
    private BigInteger minimumAmount;
    @Column("maximum_amount")
    private BigInteger maximumAmount;
    @Column("interest_rate")
    private BigDecimal interestRate;
    @Column("automatic_validation")
    private Boolean automaticValidation;
    @Column("created_at")
    private java.time.OffsetDateTime createdAt;
    @Column("updated_at")
    private java.time.OffsetDateTime updatedAt;

}
