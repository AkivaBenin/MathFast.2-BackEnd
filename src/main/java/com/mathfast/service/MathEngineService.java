package com.mathfast.service;

import com.mathfast.dto.QuestionDto;
import com.mathfast.enums.Path;
import org.springframework.stereotype.Service;

import java.util.concurrent.ThreadLocalRandom;

@Service
public class MathEngineService {

    private static final String[] NAMES = {"דני", "יוסי", "רחל", "נועה"};
    private static final String[] OBJECTS = {"תפוחים", "כדורים", "ספרים", "עפרונות"};
    
    private static final String ALPHANUMERIC = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final int NONCE_LENGTH = 20;

    public QuestionDto generateQuestion(Path requestedPath, boolean hasUsedShortcut) {
        // The "Lock" rule for the HIGHWAY shortcut
        if (requestedPath == Path.HIGHWAY && hasUsedShortcut) {
            requestedPath = Path.REGULAR;
        }

        String questionText;
        int solution;
        int pointValue;

        switch (requestedPath) {
            case HIGHWAY:
                int a = ThreadLocalRandom.current().nextInt(2, 13);
                int b = ThreadLocalRandom.current().nextInt(2, 13);
                int c = ThreadLocalRandom.current().nextInt(1, 51);
                questionText = String.format("(%d * %d) + %d = ?", a, b, c);
                solution = (a * b) + c;
                pointValue = 30; // Massive boost
                break;

            case DIRT_ROAD:
                int x = ThreadLocalRandom.current().nextInt(1, 11);
                int y = ThreadLocalRandom.current().nextInt(1, 11);
                questionText = String.format("%d + %d = ?", x, y);
                solution = x + y;
                pointValue = 7; // Low-risk addition
                break;

            case REGULAR:
            default:
                String name = NAMES[ThreadLocalRandom.current().nextInt(NAMES.length)];
                String object = OBJECTS[ThreadLocalRandom.current().nextInt(OBJECTS.length)];
                int startAmount = ThreadLocalRandom.current().nextInt(5, 21);
                int addedAmount = ThreadLocalRandom.current().nextInt(2, 16);
                questionText = String.format("ל%s היו %d %s, לאחר מכן התקבלו עוד %d. כמה %s יש עכשיו?", 
                                             name, startAmount, object, addedAmount, object);
                solution = startAmount + addedAmount;
                pointValue = 10; // Standard localized logic
                break;
        }

        return QuestionDto.builder()
                .questionText(questionText)
                .solution(solution)
                .pointValue(pointValue)
                .nonce(generateNonce())
                .build();
    }

    private String generateNonce() {
        StringBuilder sb = new StringBuilder(NONCE_LENGTH);
        for (int i = 0; i < NONCE_LENGTH; i++) {
            sb.append(ALPHANUMERIC.charAt(ThreadLocalRandom.current().nextInt(ALPHANUMERIC.length())));
        }
        return sb.toString();
    }
}
