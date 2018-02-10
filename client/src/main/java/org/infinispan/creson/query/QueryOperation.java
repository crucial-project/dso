package org.infinispan.creson.query;

import org.infinispan.client.hotrod.configuration.ClientIntelligence;
import org.infinispan.client.hotrod.impl.operations.RetryOnFailureOperation;
import org.infinispan.client.hotrod.impl.protocol.Codec;
import org.infinispan.client.hotrod.impl.protocol.HeaderParams;
import org.infinispan.client.hotrod.impl.transport.Transport;
import org.infinispan.client.hotrod.impl.transport.TransportFactory;
import org.infinispan.client.hotrod.impl.transport.tcp.TcpTransportFactory;
import org.infinispan.commons.marshall.Marshaller;
import org.infinispan.marshall.core.JBossMarshaller;


import java.io.IOException;
import java.net.SocketAddress;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public class QueryOperation extends RetryOnFailureOperation<CresonResponse> {

    private final RemoteQuery remoteQuery;

    public QueryOperation(Codec codec, TransportFactory transportFactory, byte[] cacheName, AtomicInteger topologyId,
                          int flags, ClientIntelligence clientIntelligence, RemoteQuery remoteQuery) {
        super(codec, transportFactory, cacheName, topologyId, flags, clientIntelligence);
        this.remoteQuery = remoteQuery;
    }

    @Override
    protected Transport getTransport(int i, Set<SocketAddress> failedServers) {
        if (!(transportFactory instanceof TcpTransportFactory)) {
            return transportFactory.getTransport(failedServers, this.cacheName);
        }
        Collection<SocketAddress> servvers = ((TcpTransportFactory) transportFactory).getServers();

        return transportFactory.getAddressTransport(servvers.iterator().next());
    }

    @Override
    protected CresonResponse executeOperation(Transport transport) {
        Marshaller marshall = new JBossMarshaller();
        HeaderParams params = writeHeader(transport, QUERY_REQUEST);
        CresonRequest request = new CresonRequest();
        request.setMaxResults(remoteQuery.maxResults);
        request.setQueryString(remoteQuery.jpqlString);
        request.setStartOffset(remoteQuery.startOffset);

        try {
            transport.writeArray(marshall.objectToByteBuffer(request));
            transport.flush();
            readHeaderAndValidate(transport, params);
            byte[] responseBytes = transport.readArray();
            return (CresonResponse) marshall.objectFromByteBuffer(responseBytes);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        } catch (InterruptedException e) {
            e.printStackTrace();
            return null;
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }


}
