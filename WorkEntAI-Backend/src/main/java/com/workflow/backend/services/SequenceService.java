package com.workflow.backend.services;

import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SequenceService {

    private final MongoTemplate mongoTemplate;

    /**
     * Atomically increments and returns the next sequence value for the given year.
     * Uses MongoDB findAndModify with $inc to guarantee uniqueness across restarts and replicas.
     *
     * @param year the four-digit year (e.g. 2026)
     * @return the next sequential integer for that year (starts at 1)
     */
    public long nextVal(int year) {
        String key = "tramite_" + year;

        Query query = new Query(Criteria.where("_id").is(key));
        Update update = new Update().inc("seq", 1);
        FindAndModifyOptions options = FindAndModifyOptions.options()
                .returnNew(true)
                .upsert(true);

        SequenceDocument result = mongoTemplate.findAndModify(
                query, update, options, SequenceDocument.class, "sequences");

        return result != null ? result.getSeq() : 1L;
    }

    @Document(collection = "sequences")
    static class SequenceDocument {
        private String id;
        private long seq;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public long getSeq() { return seq; }
        public void setSeq(long seq) { this.seq = seq; }
    }
}
