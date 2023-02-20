package no.fintlabs.model;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.MediaType;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class File {

    @NotBlank
    private String name;

    @NotNull
    private Long sourceApplicationId;

    @NotBlank
    private String sourceApplicationInstanceId;

    @JsonSerialize(using = ToStringSerializer.class)
    private MediaType type;

    private String encoding;

    @NotEmpty
    private byte[] contents;

}
