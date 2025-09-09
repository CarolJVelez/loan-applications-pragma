package co.com.bancolombia.usecase;

import co.com.bancolombia.model.client.UserClientDetails;
import co.com.bancolombia.model.exceptions.NotFoundException;
import co.com.bancolombia.model.loanApplication.LoanApplication;
import co.com.bancolombia.model.loanApplication.PageResult;
import co.com.bancolombia.model.loanApplication.gateways.LoanApplicationRepository;
import co.com.bancolombia.model.loanApplication.gateways.LoggerRepository;
import co.com.bancolombia.model.status.gateways.StatusRepository;
import co.com.bancolombia.usecase.client.IUserClient;
import co.com.bancolombia.usecase.statusLoan.StatusUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigInteger;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StatusUseCaseTest {

    @Mock private LoanApplicationRepository loanApplicationRepository;
    @Mock private StatusRepository statusRepository;
    @Mock private LoggerRepository logger;
    @Mock private IUserClient userClient;

    @InjectMocks
    private StatusUseCase statusUseCase;

    private LoanApplication l1;
    private LoanApplication l2;

    @BeforeEach
    void init() {
        l1 = LoanApplication.builder()
                .loanApplicationId(1L).userId(10L)
                .email("a@x.com").amount(BigInteger.valueOf(1_000_000))
                .loanType("LIBRE_INVERSION").status("APPROVED").build();
        l2 = LoanApplication.builder()
                .loanApplicationId(2L).userId(20L)
                .email("b@x.com").amount(BigInteger.valueOf(2_000_000))
                .loanType("LIBRE_INVERSION").status("APPROVED").build();
    }

    @Test
    void list_nullStates_shouldError() {
        StepVerifier.create(statusUseCase.list(0, 10, null))
                .expectError(NotFoundException.class)
                .verify();
    }

    @Test
    void list_emptyStates_shouldError() {
        StepVerifier.create(statusUseCase.list(0, 10, Set.of()))
                .expectError(NotFoundException.class)
                .verify();
    }

    @Test
    void list_missingStatus_shouldError() {
        when(statusRepository.existsByName("APPROVED")).thenReturn(Mono.just(true));
        when(statusRepository.existsByName("REJECTED")).thenReturn(Mono.just(false));

        StepVerifier.create(statusUseCase.list(0, 10, Set.of("APPROVED", "REJECTED")))
                .expectError(NotFoundException.class)
                .verify();
    }

    @Test
    void list_ok_shouldReturnPageAndMapUsers() {
        when(statusRepository.existsByName(anyString())).thenReturn(Mono.just(true));
        when(loanApplicationRepository.findByStatuses(anyCollection(), eq(0), eq(10)))
                .thenReturn(Flux.just(l1.toBuilder().build(), l2.toBuilder().build()));
        when(loanApplicationRepository.countByStatuses(anyCollection()))
                .thenReturn(Mono.just(2L));

        UserClientDetails u1 = UserClientDetails.builder()
                .userId(10L).name("Ana").lastName("Lopez").baseSalary(BigInteger.valueOf(9_000_000)).build();
        when(userClient.findByIds(anyList())).thenReturn(Flux.just(u1));

        StepVerifier.create(statusUseCase.list(0, 10, Set.of("APPROVED")))
                .assertNext((PageResult<LoanApplication> page) -> {
                    assertEquals(2, page.getContent().size());
                    assertEquals(0, page.getPage());
                    assertEquals(10, page.getSize());
                    assertEquals(2L, page.getTotalElements());
                    LoanApplication a = page.getContent().get(0);
                    LoanApplication b = page.getContent().get(1);
                    assertEquals(BigInteger.valueOf(9_000_000), a.getBaseSalary());
                    assertEquals("Ana Lopez", a.getNames());
                    assertNull(b.getBaseSalary());
                })
                .verifyComplete();

        verify(logger, atLeastOnce()).info(anyString(), any());
        verify(logger, atLeast(0)).warn(anyString(), any());
    }

    @Test
    void list_ok_whenNoUserIds_shouldSkipAuthCall() {
        when(statusRepository.existsByName(anyString())).thenReturn(Mono.just(true));
        LoanApplication a = l1.toBuilder().userId(null).build();
        LoanApplication b = l2.toBuilder().userId(null).build();
        when(loanApplicationRepository.findByStatuses(anyCollection(), eq(0), eq(10)))
                .thenReturn(Flux.just(a, b));
        when(loanApplicationRepository.countByStatuses(anyCollection())).thenReturn(Mono.just(2L));

        StepVerifier.create(statusUseCase.list(0, 10, Set.of("APPROVED")))
                .assertNext(page -> {
                    assertEquals(2, page.getContent().size());
                    assertNull(page.getContent().get(0).getUserId());
                    assertNull(page.getContent().get(1).getUserId());
                })
                .verifyComplete();

        verifyNoInteractions(userClient);
    }
}
