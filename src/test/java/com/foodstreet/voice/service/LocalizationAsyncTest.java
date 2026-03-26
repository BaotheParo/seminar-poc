package com.foodstreet.voice.service;

import com.foodstreet.voice.entity.FoodStall;
import com.foodstreet.voice.entity.FoodStallLocalization;
import com.foodstreet.voice.repository.FoodStallLocalizationRepository;
import com.foodstreet.voice.repository.FoodStallRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Pure Mockito unit tests for LocalizationService.processLocalizationAndAudioInBackground.
 * No Spring context, no DB required — tests can run offline.
 *
 * The @Async proxy is bypassed (we call the method directly on the real instance),
 * so assertions are synchronous and deterministic.
 */
@ExtendWith(MockitoExtension.class)
class LocalizationAsyncTest {

    @Mock  private TranslationService translationService;
    @Mock  private AudioService audioService;
    @Mock  private FoodStallLocalizationRepository localizationRepository;
    @Mock  private FoodStallRepository foodStallRepository;   // required by @InjectMocks

    @InjectMocks
    private LocalizationService localizationService;

    private FoodStall dummyStall;

    @BeforeEach
    void setUp() {
        dummyStall = FoodStall.builder()
                .name("Quán Ốc Oanh")
                .description("Quán ốc ngon nhất Sài Gòn")
                .build();
        ReflectionTestUtils.setField(dummyStall, "id", 99L);

        // Translate returns a predictable prefix so we can verify calls
        lenient().when(translationService.translate(anyString(), anyString()))
                .thenAnswer(inv -> "[" + inv.getArgument(1) + "] " + inv.getArgument(0));

        // Audio service returns a URL
        lenient().when(audioService.getOrCreateAudioForStall(anyLong(), anyString(), anyString()))
                .thenAnswer(inv -> "/audio/" + inv.getArgument(0) + "_" + inv.getArgument(2) + ".mp3");

        // No existing row → new entity path
        lenient().when(localizationRepository.findByFoodStallIdAndLanguageCode(anyLong(), anyString()))
                .thenReturn(Optional.empty());

        lenient().when(localizationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    @DisplayName("LOC-01: Happy path — all 5 languages get a localization row saved")
    void shouldSaveLocalizationForAllFiveLanguages() {
        localizationService.processLocalizationAndAudioInBackground(dummyStall);

        // One save per language
        verify(localizationRepository, times(5)).save(any(FoodStallLocalization.class));

        // "vi" must NOT call translate
        verify(translationService, never()).translate(anyString(), eq("vi"));

        // Each of the other 4 languages triggers exactly 2 translate calls (name + description)
        verify(translationService, times(2)).translate(anyString(), eq("en"));
        verify(translationService, times(2)).translate(anyString(), eq("ja"));
        verify(translationService, times(2)).translate(anyString(), eq("ko"));
        verify(translationService, times(2)).translate(anyString(), eq("zh"));

        // Audio generated for all 5 languages
        verify(audioService, times(5)).getOrCreateAudioForStall(eq(99L), anyString(), anyString());
    }

    @Test
    @DisplayName("LOC-02: Fault isolation — translate throws for 'ko', but 'zh' still succeeds")
    void shouldContinueToNextLanguageWhenOneTranslationFails() {
        when(translationService.translate(anyString(), eq("ko")))
                .thenThrow(new RuntimeException("MyMemory API timeout for ko"));

        localizationService.processLocalizationAndAudioInBackground(dummyStall);

        // "ko" row not saved (exception in translate phase), other 4 must be saved
        verify(localizationRepository, times(4)).save(any(FoodStallLocalization.class));

        // "zh" must still be attempted despite "ko" failing before it
        verify(translationService, atLeastOnce()).translate(anyString(), eq("zh"));
        verify(audioService, times(1)).getOrCreateAudioForStall(eq(99L), anyString(), eq("zh"));
    }
}
