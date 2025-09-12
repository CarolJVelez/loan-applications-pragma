package co.com.bancolombia.model.notifications;

import lombok.*;

import java.time.OffsetDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class MessageSQS {
    private String fullName;
    private String status;
    private String email;
    private String observations;
    private OffsetDateTime updatedAt;
}
