package com.onuriltan.twitteranalyzerserver.base;

import com.onuriltan.twitteranalyzerserver.websocket.model.TokenizedTweet;
import com.onuriltan.twitteranalyzerserver.websocket.model.Tweet;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.*;

import edu.stanford.nlp.util.CoreMap;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.List;


@Service
public class TweetAnalyzer {

    @Inject
    private SimpMessageSendingOperations webSocket;

    @Inject
    RedisTemplate redisTemplate;

    @Inject
    StanfordCoreNLP stanfordCoreNLP;

    public void applyNLP() {

        Tweet tweet = (Tweet) redisTemplate.boundListOps("tweet").leftPop();

        Annotation document = new Annotation(tweet.getTweet());

        // run all Annotators on this text
        stanfordCoreNLP.annotate(document);

        List<CoreMap> sentences = document.get(CoreAnnotations.SentencesAnnotation.class);

        for (CoreMap sentence : sentences) {
            // traversing the words in the current sentence
            // a CoreLabel is a CoreMap with additional token-specific methods
            for (CoreLabel token : sentence.get(CoreAnnotations.TokensAnnotation.class)) {
                // this is the text of the token
                String word = token.get(CoreAnnotations.TextAnnotation.class);

                // this is the NER label of the token
                String ne = token.get(CoreAnnotations.NamedEntityTagAnnotation.class);
                if(!ne.equals("O") && !ne.equals("MISC")){
                    TokenizedTweet tokenizedTweet = new TokenizedTweet();
                    tokenizedTweet.setWord(word);
                    tokenizedTweet.setNamedEntity(ne);
                    System.out.println(String.format("word: [%s] ne: [%s]", word, ne));
                    System.out.println();
                    webSocket.convertAndSend("/topic/fetchTwitterStream", tokenizedTweet);

                }

            }

        }


    }
}
