package no.fintlabs.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class File {

    @NotNull
    private UUID id;

    @NotBlank
    private String name;

    @NotNull
    private Long sourceApplicationId;

    @NotBlank
    private String sourceApplicationInstanceId;

    private String type;

    private String encoding;

    @NotEmpty
    private byte[] contents;

}
