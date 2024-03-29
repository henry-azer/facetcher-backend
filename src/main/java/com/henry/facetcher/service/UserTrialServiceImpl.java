package com.henry.facetcher.service;

import com.henry.facetcher.dao.UserTrialDao;
import com.henry.facetcher.dto.UserSubmissionDto;
import com.henry.facetcher.dto.UserTrialDto;
import com.henry.facetcher.manager.JWTAuthenticationManager;
import com.henry.facetcher.model.UserTrial;
import com.henry.facetcher.processor.FDLProcessor;
import com.henry.facetcher.transformer.UserTrialTransformer;
import com.henry.facetcher.util.FDLGenerator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import javax.persistence.EntityExistsException;
import javax.persistence.EntityNotFoundException;
import javax.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static com.henry.facetcher.constants.FacetcherConstants.*;

/**
 * @author Henry Azer
 * @since 31/01/2023
 */
@Slf4j
@Service
public class UserTrialServiceImpl implements UserTrialService {
    private final ImageService imageService;
    private final UserService userService;
    private final UserTrialTransformer userTrialTransformer;
    private final UserTrialDao userTrialDao;
    private final FDLProcessor fdlProcessor;
    private final JWTAuthenticationManager authenticationManager;
    private final UserSubmissionService userSubmissionService;
    private final ConfigValueService configValueService;

    public UserTrialServiceImpl(UserTrialTransformer userTrialTransformer, UserTrialDao userTrialDao, FDLProcessor fdlProcessor,
                                JWTAuthenticationManager authenticationManager, @Lazy UserSubmissionService userSubmissionService,
                                ImageService imageService, UserService userService, ConfigValueService configValueService) {
        this.userTrialTransformer = userTrialTransformer;
        this.userTrialDao = userTrialDao;
        this.fdlProcessor = fdlProcessor;
        this.authenticationManager = authenticationManager;
        this.userSubmissionService = userSubmissionService;
        this.imageService = imageService;
        this.userService = userService;
        this.configValueService = configValueService;
    }

    @Override
    public UserTrialTransformer getTransformer() {
        return userTrialTransformer;
    }

    @Override
    public UserTrialDao getDao() {
        return userTrialDao;
    }

    @Override
    @Transactional
    public UserTrialDto findById(Long userTrialId) {
        log.info("UserTrialService: findById() called");
        Optional<UserTrial> optionalUserTrial = getDao().findById(userTrialId);
        if (optionalUserTrial.isEmpty()) throw new EntityExistsException("User trial not exists for id: " + userTrialId);
        return getTransformer().transformEntityToDto(optionalUserTrial.get());
    }

    @Override
    public UserTrialDto create(UserTrialDto dto) {
        log.info("UserTrialService: create() called");
        UserTrial transformedDtoToEntity = getTransformer().transformDtoToEntity(dto);
        transformedDtoToEntity.setUser(userService.getTransformer().transformDtoToEntity(userService.getCurrentUser()));
        return getTransformer().transformEntityToDto(getDao().create(transformedDtoToEntity));
    }

    @Override
    public List<UserTrialDto> findAllUserTrials() {
        log.info("UserTrialService: findAllUserTrials() called");
        return getTransformer().transformEntityToDto(getDao().findAllUserTrials());
    }

    @Override
    public List<UserTrialDto> findAllFailedUserTrials() {
        log.info("UserTrialService: findAllFailedUserTrials() called");
        return getTransformer().transformEntityToDto(getDao().findAllFailedUserTrials());
    }

    @Override
    public List<UserTrialDto> findAllSucceededUserTrials() {
        log.info("UserTrialService: findAllSucceededUserTrials() called");
        return getTransformer().transformEntityToDto(getDao().findAllSucceededUserTrials());
    }

    @Override
    public List<UserTrialDto> findAllUserTrialsByUserId(Long userId) {
        log.info("UserTrialService: findAllUserTrialsByUserId() called");
        return getTransformer().transformEntityToDto(getDao().findAllUserTrialsByUserId(userId));
    }

    @Override
    public List<UserTrialDto> findAllUserTrialsByUserSubmissionId(Long userSubmissionId) {
        log.info("UserTrialService: findAllUserTrialsByUserSubmissionId() called");
        return getTransformer().transformEntityToDto(getDao().findAllUserTrialsByUserSubmissionId(userSubmissionId));
    }

    @Override
    public Long findSucceededUserTrialsCountByUserId(Long userId) {
        log.info("UserTrialService: findSucceededUserTrialsCountByUserId() called");
        return getDao().findSucceededUserTrialsCountByUserId(userId);
    }

    @Override
    public Long findFailedUserTrialsCountByUserId(Long userId) {
        log.info("UserTrialService: findFailedUserTrialsCountByUserId() called");
        return getDao().findFailedUserTrialsCountByUserId(userId);
    }

    @Override
    public UserTrialDto processUserTrial(UserTrialDto userTrialDto) {
        log.info("UserTrialService: processUserTrial() called");
        userTrialDto.setUserSubmission(userSubmissionService.findById(userTrialDto.getUserSubmissionId()));
        userTrialDto.setGender(userTrialDto.getUserSubmission().getGender());
        userTrialDto.setInputImage(imageService.create(imageService.constructImageDto(userTrialDto.getInputImageFile(), configValueService.findConfigValueByConfigKey(FII_BUCKET), configValueService.findConfigValueByConfigKey(FII_CDN))));
        userTrialDto.setProcessProperties(FDLGenerator.generateFDLImageProperties(userTrialDto.getInputImage().getImageUrl(), userTrialDto.getGender(), configValueService.findConfigValueByConfigKey(ACCESS_KEY),
                configValueService.findConfigValueByConfigKey(SECRET_KEY), configValueService.findConfigValueByConfigKey(FIO_BUCKET), configValueService.findConfigValueByConfigKey(FIO_CDN), configValueService.findConfigValueByConfigKey(REGION_STATIC)));userTrialDto.setImageProperties(userTrialDto.getProcessProperties().toString());
        String outputURL = fdlProcessor.process(userTrialDto);
        userTrialDto.setTrialDate(LocalDateTime.now());
        if (!outputURL.isEmpty()) userTrialDto.setOutputImage(imageService.create(imageService.constructImageDto(userTrialDto.getInputImage().getName(), outputURL)));
        userTrialDto.setTrailMessage(authenticationManager.getCurrentUserEmail() + configValueService.findConfigValueByConfigKey(TRIAL_MESSAGE) + userTrialDto.getTrialDate());
        userTrialDto.setDescription(userTrialDto.getUserSubmission().getDescription());
        userTrialDto.setTitle(userTrialDto.getUserSubmission().getTitle());
        UserTrialDto dbUserTrialDto = create(userTrialDto);
        if (userTrialDto.getExceptionOccurred()) throw new EntityNotFoundException("Invalid FDL processor image input, Try again!");
        return dbUserTrialDto;
    }

    @Override
    public UserSubmissionDto submitUserTrialById(Long userTrialId) {
        log.info("UserTrialService: processUserTrial() called");
        return userSubmissionService.submitUserSubmissionByUserTrialId(userTrialId);
    }
}
