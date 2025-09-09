package co.com.bancolombia.model.client;

import lombok.*;

import java.math.BigInteger;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class UserClientDetails {

    private Long userId;
    private String name;
    private String lastName;
    private String document;
    private LocalDate birthDate;
    private String address;
    private String phone;
    private String email;
    private BigInteger baseSalary;
}
