package org.crucial.dso;

public class Sum implements RemoteBiFunction<Long,Long,Long> {

    @Override
    public Long apply(Long v1, Long v2) {
        return v1+v2;
    }

}


