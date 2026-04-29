package be.loic.tfe_cobblemon.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.dataset-import")
public class DatasetImportProperties {

    @NotBlank
    private String code;

    @NotBlank
    private String label;

    @NotBlank
    private String inputPath;

    private boolean cleanBeforeImport = true;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getInputPath() {
        return inputPath;
    }

    public void setInputPath(String inputPath) {
        this.inputPath = inputPath;
    }

    public boolean isCleanBeforeImport() {
        return cleanBeforeImport;
    }

    public void setCleanBeforeImport(boolean cleanBeforeImport) {
        this.cleanBeforeImport = cleanBeforeImport;
    }
}