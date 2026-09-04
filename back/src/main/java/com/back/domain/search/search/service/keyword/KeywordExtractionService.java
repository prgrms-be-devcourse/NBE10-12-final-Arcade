package com.back.domain.search.search.service.keyword;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.ko.KoreanTokenizer;
import org.apache.lucene.analysis.ko.POS;
import org.apache.lucene.analysis.ko.dict.UserDictionary;
import org.apache.lucene.analysis.ko.tokenattributes.PartOfSpeechAttribute;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import jakarta.annotation.PreDestroy;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import static org.apache.lucene.analysis.TokenStream.DEFAULT_TOKEN_ATTRIBUTE_FACTORY;

@Component
public class KeywordExtractionService implements KeywordExtractionPort {

    private static final String USER_DICTIONARY_PATH = "nori/user-dictionary.txt";

    private static final Set<POS.Tag> KEYWORD_TAGS = EnumSet.of(
            POS.Tag.NNG, POS.Tag.NNP,
            POS.Tag.SL, POS.Tag.SH, POS.Tag.SN
    );

    private final Analyzer analyzer;

    public KeywordExtractionService() {
        this.analyzer = buildAnalyzer(loadUserDictionary());
    }

    @Override
    public List<String> extract(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }

        Set<String> keywords = new LinkedHashSet<>();
        try (TokenStream tokenStream = analyzer.tokenStream("content", text)) {
            CharTermAttribute termAtt = tokenStream.addAttribute(CharTermAttribute.class);
            PartOfSpeechAttribute posAtt = tokenStream.addAttribute(PartOfSpeechAttribute.class);

            tokenStream.reset();
            while (tokenStream.incrementToken()) {
                if (KEYWORD_TAGS.contains(posAtt.getLeftPOS())) {
                    keywords.add(termAtt.toString().toLowerCase(Locale.ROOT));
                }
            }
            tokenStream.end();
        } catch (IOException e) {
            throw new IllegalStateException("키워드 추출 중 오류가 발생했습니다.", e);
        }

        return List.copyOf(keywords);
    }

    @PreDestroy
    public void close() {
        analyzer.close();
    }

    private Analyzer buildAnalyzer(UserDictionary userDictionary) {
        return new Analyzer() {
            @Override
            protected TokenStreamComponents createComponents(String fieldName) {
                Tokenizer tokenizer = new KoreanTokenizer(
                        DEFAULT_TOKEN_ATTRIBUTE_FACTORY,
                        userDictionary,
                        KoreanTokenizer.DecompoundMode.MIXED,
                        false
                );
                return new TokenStreamComponents(tokenizer);
            }
        };
    }

    private UserDictionary loadUserDictionary() {
        try (InputStreamReader reader = new InputStreamReader(
                new ClassPathResource(USER_DICTIONARY_PATH).getInputStream(), StandardCharsets.UTF_8)) {
            return UserDictionary.open(reader);
        } catch (IOException e) {
            throw new IllegalStateException("Nori 사용자 사전을 불러오지 못했습니다: " + USER_DICTIONARY_PATH, e);
        }
    }
}
