package co.com.bancolombia.model.status;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class LoanStatus {

    private Long statusId;
    private String code;
    private String name;
}
