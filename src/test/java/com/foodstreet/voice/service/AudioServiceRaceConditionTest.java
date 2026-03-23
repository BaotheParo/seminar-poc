package com.foodstreet.voice.service;

import com.foodstreet.voice.service.audio.AudioProviderStrategy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@SpringBootTest
@org.springframework.test.context.TestPropertySource(properties = {
    "VIETMAP_API_KEY_SERVICES=dummy",
    "AUDIO_BASE_URL=http://localhost"
})
public class AudioServiceRaceConditionTest {

    @Autowired
    private AudioService audioService;

    // Mock cái Strategy để không gọi Google/Edge TTS thật tốn tiền lúc chạy test
    @MockBean
    private AudioProviderStrategy audioProviderStrategy;

    @Test
    @DisplayName("RC-01: 10 Threads cùng gọi 1 file Audio -> Chỉ gọi TTS API 1 lần duy nhất")
    void shouldCoalesceConcurrentRequests() throws Exception {
        // Giả lập: Khi gọi TTS, tốn 2 giây mới xong và trả về mảng byte giả
        when(audioProviderStrategy.generateAudio(anyString(), anyString()))
                .thenAnswer(invocation -> {
                    Thread.sleep(2000); // Giả lập độ trễ mạng
                    return new byte[]{1, 2, 3}; // Dummy audio data
                });

        int numberOfThreads = 10;
        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch startLatch = new CountDownLatch(1); // Để ép 10 thread xuất phát cùng 1 miligiây
        CountDownLatch doneLatch = new CountDownLatch(numberOfThreads); // Đợi 10 thread làm xong

        List<Future<String>> futures = new ArrayList<>();

        for (int i = 0; i < numberOfThreads; i++) {
            futures.add(executorService.submit(() -> {
                startLatch.await(); // Các thread đứng đây đợi hiệu lệnh
                // Cả 10 thread cùng đòi sinh audio tiếng Anh
                String result = audioService.getOrCreateAudio("Hello Ốc Oanh", "en");
                doneLatch.countDown();
                return result;
            }));
        }

        // BẮT ĐẦU! Cho 10 thread cùng chạy ập vào hệ thống
        startLatch.countDown();
        
        // Đợi tất cả chạy xong
        doneLatch.await(10, TimeUnit.SECONDS);

        // KỂT QUẢ KIỂM TRA (Assertions)
        // 1. Chắc chắn 10 người đều nhận được URL giống nhau
        String expectedUrl = futures.get(0).get();
        for (Future<String> future : futures) {
            assertEquals(expectedUrl, future.get());
        }

        // 2. QUAN TRỌNG NHẤT: Verify hàm gọi TTS thật (Google/Edge) CHỈ ĐƯỢC GỌI ĐÚNG 1 LẦN
        verify(audioProviderStrategy, times(1)).generateAudio(anyString(), anyString());

        executorService.shutdown();
    }
}
