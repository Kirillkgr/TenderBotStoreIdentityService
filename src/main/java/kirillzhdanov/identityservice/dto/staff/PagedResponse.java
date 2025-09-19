package kirillzhdanov.identityservice.dto.staff;

import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PagedResponse<T> {
    private List<T> items;
    private long total;
    private int page;
    private int size;
}
