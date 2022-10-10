package no.fintlabs.file;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class File {
    @NotBlank
    private String name;
    @NotBlank
    private String type;
    @NotBlank
    private String encoding;
    @NotEmpty
    private byte[] contents;
}
