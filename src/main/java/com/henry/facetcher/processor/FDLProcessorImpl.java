package com.henry.facetcher.processor;

import com.henry.facetcher.dto.UserTrialDto;
import com.henry.facetcher.service.ConfigValueService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.persistence.EntityNotFoundException;
import java.io.*;
import java.util.List;

import static com.henry.facetcher.constants.FacetcherConstants.*;

/**
 * @author Henry Azer
 * @since 12/02/2023
 */
@Slf4j
@Component
public class FDLProcessorImpl implements FDLProcessor {
    private final ConfigValueService configValueService;

    public FDLProcessorImpl(ConfigValueService configValueService) {
        this.configValueService = configValueService;
    }

    @Override
    public String process(UserTrialDto userTrialDto) {
        log.info("FDLProcessor: process() called");
        try {
            ProcessBuilder processBuilder = buildFDLProcessBuilder(userTrialDto.getProcessProperties());
            processBuilder.directory(new File(System.getProperties().get("user.dir") + configValueService.findConfigValueByConfigKey(FDL_DIRECTORY_PATH)));
            Process process = processBuilder.start();

            log.info("FDLProcessor: process() started");
            BufferedReader processOutputReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String outputURL = "";
            String processOutputLine;
            boolean exceptionStored = false;
            userTrialDto.setExceptionOccurred(false);
            while ((processOutputLine = processOutputReader.readLine()) != null) {
                if (userTrialDto.getExceptionOccurred()) {
                    if (!exceptionStored) {
                        userTrialDto.setExceptionOccurred(true);
                        userTrialDto.setExceptionMessage(processOutputLine);
                        exceptionStored = true;
                    }
                }
                if (processOutputLine.equals(FDL_EXCEPTION))
                    userTrialDto.setExceptionOccurred(true);
                if (processOutputLine.contains(FDL_OUTPUT_FILE))
                    outputURL = processOutputLine.replace(FDL_OUTPUT_FILE + " ", "");
                System.out.println("FDL Process Output: " + processOutputLine);
            }

            BufferedReader processErrorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            String processErrorLine;
            while ((processErrorLine = processErrorReader.readLine()) != null)
                System.out.println("FDL Process Error: " + processErrorLine);

            int exitCode = process.waitFor();
            if (exitCode != 0 || (!userTrialDto.getExceptionOccurred() && outputURL.isEmpty()))
                throw new IllegalStateException("Invalid FDL processor image input, Try again!");
            log.info("FDL Process Output: Exit Code: " + exitCode);
            log.info("FDLProcessor: process() ended");

            return outputURL;
        } catch (Exception exception) {
            throw new EntityNotFoundException("Invalid FDL processor image input, Try again!");
        }
    }

    private ProcessBuilder buildFDLProcessBuilder(List<String> properties) {
        log.info("FDLProcessor: buildFDLProcessBuilder() called");
        return new ProcessBuilder("python3", System.getProperties().get("user.dir") +
                configValueService.findConfigValueByConfigKey(FDL_PATH), String.join(" ", properties));
    }
}
