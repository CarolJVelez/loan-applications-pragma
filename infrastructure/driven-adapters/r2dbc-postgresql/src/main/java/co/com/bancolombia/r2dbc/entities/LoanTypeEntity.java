package co.com.bancolombia.r2dbc.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(schema = "loan_type")
public class LoanTypeEntity {

    @Id
    @Column(name = "id")
    private Long loanTypeId;
    private String code;
    private String name;
    private Boolean active;
    @Column(name = "created_at")
    private java.time.OffsetDateTime createdAt;
}
