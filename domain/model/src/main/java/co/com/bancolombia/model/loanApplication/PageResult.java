package co.com.bancolombia.model.loanApplication;

@lombok.Value
public class PageResult<T> {
    java.util.List<T> content;
    int page;
    int size;
    long totalElements;
}
