package no.fintlabs.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.*;
import lombok.extern.jackson.Jacksonized;
import no.fintlabs.Base64Deserializer;
import org.springframework.http.MediaType;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

@Getter
@EqualsAndHashCode
@Jacksonized
@Builder
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
    @JsonDeserialize(using = Base64Deserializer.class)
    private byte[] contents;

}
