package co.com.bancolombia.r2dbc.entities;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Table("status")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LoanStatusEntity {

    @Id
    private Long statusId;
    private String code;
    private String name;
}
