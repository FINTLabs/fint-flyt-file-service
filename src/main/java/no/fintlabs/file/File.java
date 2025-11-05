package no.fintlabs.file;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;
import no.fintlabs.Base64Deserializer;
import org.springframework.http.MediaType;

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
