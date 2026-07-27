package com.health.health_system.service;

import com.health.health_system.entity.SensitiveWord;
import com.health.health_system.repository.SensitiveWordRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class SensitiveWordFilterService {

    @Autowired
    private SensitiveWordRepository sensitiveWordRepository;

    private Set<String> blockWords = new HashSet<>();
    private Set<String> warningWords = new HashSet<>();
    private Pattern blockPattern;
    private Pattern warningPattern;

    // 敏感词替换字符
    private static final String REPLACEMENT = "**";

    @PostConstruct
    public void init() {
        loadSensitiveWords();
    }

    /**
     * 加载敏感词到内存
     */
    public void loadSensitiveWords() {
        List<SensitiveWord> words = sensitiveWordRepository.findAll();

        blockWords.clear();
        warningWords.clear();

        for (SensitiveWord word : words) {
            if ("BLOCK".equals(word.getLevel())) {
                blockWords.add(word.getWord().toLowerCase());
            } else {
                warningWords.add(word.getWord().toLowerCase());
            }
        }

        // 编译正则表达式
        if (!blockWords.isEmpty()) {
            String blockRegex = blockWords.stream()
                    .map(Pattern::quote)
                    .collect(Collectors.joining("|"));
            blockPattern = Pattern.compile(blockRegex, Pattern.CASE_INSENSITIVE);
        }

        if (!warningWords.isEmpty()) {
            String warningRegex = warningWords.stream()
                    .map(Pattern::quote)
                    .collect(Collectors.joining("|"));
            warningPattern = Pattern.compile(warningRegex, Pattern.CASE_INSENSITIVE);
        }
    }

    /**
     * 检查文本是否包含敏感词
     * @param text 待检查的文本
     * @return 包含的敏感词列表（空列表表示没有敏感词）
     */
    public List<String> checkSensitiveWords(String text) {
        if (text == null || text.isEmpty()) {
            return new ArrayList<>();
        }

        String lowerText = text.toLowerCase();
        List<String> foundWords = new ArrayList<>();

        // 检查禁止词
        for (String word : blockWords) {
            if (lowerText.contains(word)) {
                foundWords.add(word);
            }
        }

        // 检查警告词
        for (String word : warningWords) {
            if (lowerText.contains(word)) {
                foundWords.add(word);
            }
        }

        return foundWords;
    }

    /**
     * 检查是否包含禁止词
     * @param text 待检查的文本
     * @return true-包含禁止词
     */
    public boolean containsBlockWord(String text) {
        if (text == null || text.isEmpty() || blockPattern == null) {
            return false;
        }
        return blockPattern.matcher(text).find();
    }

    /**
     * 检查是否包含警告词
     */
    public boolean containsWarningWord(String text) {
        if (text == null || text.isEmpty() || warningPattern == null) {
            return false;
        }
        return warningPattern.matcher(text).find();
    }

    /**
     * 过滤敏感词（将敏感词替换为**）
     */
    public String filterSensitiveWords(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        String result = text;

        // 替换禁止词
        for (String word : blockWords) {
            result = result.replaceAll("(?i)" + Pattern.quote(word), REPLACEMENT);
        }

        // 替换警告词
        for (String word : warningWords) {
            result = result.replaceAll("(?i)" + Pattern.quote(word), REPLACEMENT);
        }

        return result;
    }

    /**
     * 验证文本是否通过敏感词检查
     * @throws RuntimeException 如果包含禁止词
     */
    public void validateText(String text) {
        if (containsBlockWord(text)) {
            throw new RuntimeException("内容包含敏感词，请修改后重试");
        }
    }

    /**
     * 添加敏感词
     */
    public void addSensitiveWord(String word, String level) {
        SensitiveWord sensitiveWord = new SensitiveWord();
        sensitiveWord.setWord(word);
        sensitiveWord.setLevel(level);
        sensitiveWord.setCreateTime(java.time.LocalDateTime.now());
        sensitiveWordRepository.save(sensitiveWord);
        loadSensitiveWords();  // 重新加载
    }

    /**
     * 删除敏感词
     */
    public void deleteSensitiveWord(Long id) {
        sensitiveWordRepository.deleteById(id);
        loadSensitiveWords();  // 重新加载
    }

    /**
     * 获取所有敏感词
     */
    public List<SensitiveWord> getAllSensitiveWords() {
        return sensitiveWordRepository.findAll();
    }
}