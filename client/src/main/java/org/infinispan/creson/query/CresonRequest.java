package org.infinispan.creson.query;

import java.io.Serializable;

public class CresonRequest implements Serializable {
    public String getQueryString() {
        return queryString;
    }

    public Long getStartOffset() {
        return startOffset;
    }

    public Integer getMaxResults() {
        return maxResults;
    }

    private String queryString;

    private Long startOffset;

    private Integer maxResults;

    public void setQueryString(String queryString) {
        this.queryString = queryString;
    }

    public void setStartOffset(Long startOffset) {
        this.startOffset = startOffset;
    }

    public void setMaxResults(Integer maxResults) {
        this.maxResults = maxResults;
    }

}
