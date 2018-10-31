package org.infinispan.crucial.test;

import org.infinispan.commons.api.BasicCache;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.infinispan.crucial.Factory.CRUCIAL_CACHE_NAME;

/**
 * @author Pierre Sutra
 */

public class FakeCache implements BasicCache{
    @Override
    public String getName() {
        return CRUCIAL_CACHE_NAME;
    }

    @Override
    public String getVersion() {
        return null;
    }

    @Override
    public int size() {
        return 0;
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public boolean containsKey(Object key) {
        return false;
    }

    @Override
    public boolean containsValue(Object value) {
        return false;
    }

    @Override
    public Object get(Object key) {
        return null;
    }

    @Override
    public Object put(Object o, Object o2) {
        return null;
    }

    @Override
    public Object put(Object o, Object o2, long l, TimeUnit timeUnit) {
        return null;
    }

    @Override
    public Object putIfAbsent(Object o, Object o2, long l, TimeUnit timeUnit) {
        return null;
    }

    @Override
    public void putAll(Map map, long l, TimeUnit timeUnit) {

    }

    @Override
    public Object replace(Object o, Object o2, long l, TimeUnit timeUnit) {
        return null;
    }

    @Override
    public boolean replace(Object o, Object o2, Object v1, long l, TimeUnit timeUnit) {
        return false;
    }

    @Override
    public Object put(Object o, Object o2, long l, TimeUnit timeUnit, long l1, TimeUnit timeUnit1) {
        return null;
    }

    @Override
    public Object putIfAbsent(Object o, Object o2, long l, TimeUnit timeUnit, long l1, TimeUnit timeUnit1) {
        return null;
    }

    @Override
    public void putAll(Map map, long l, TimeUnit timeUnit, long l1, TimeUnit timeUnit1) {

    }

    @Override
    public Object replace(Object o, Object o2, long l, TimeUnit timeUnit, long l1, TimeUnit timeUnit1) {
        return null;
    }

    @Override
    public boolean replace(Object o, Object o2, Object v1, long l, TimeUnit timeUnit, long l1, TimeUnit timeUnit1) {
        return false;
    }

    @Override
    public Object remove(Object o) {
        return null;
    }

    @Override
    public void putAll(Map m) {

    }

    @Override
    public void clear() {

    }

    @Override
    public Set keySet() {
        return null;
    }

    @Override
    public Collection values() {
        return null;
    }

    @Override
    public Set<Entry> entrySet() {
        return null;
    }

    @Override
    public Object putIfAbsent(Object key, Object value) {
        return null;
    }

    @Override
    public boolean remove(Object key, Object value) {
        return false;
    }

    @Override
    public boolean replace(Object key, Object oldValue, Object newValue) {
        return false;
    }

    @Override
    public Object replace(Object key, Object value) {
        return null;
    }

    @Override
    public CompletableFuture putAsync(Object o, Object o2) {
        return null;
    }

    @Override
    public CompletableFuture putAsync(Object o, Object o2, long l, TimeUnit timeUnit) {
        return null;
    }

    @Override
    public CompletableFuture putAsync(Object o, Object o2, long l, TimeUnit timeUnit, long l1, TimeUnit timeUnit1) {
        return null;
    }

    @Override
    public CompletableFuture<Void> putAllAsync(Map map) {
        return null;
    }

    @Override
    public CompletableFuture<Void> putAllAsync(Map map, long l, TimeUnit timeUnit) {
        return null;
    }

    @Override
    public CompletableFuture<Void> putAllAsync(Map map, long l, TimeUnit timeUnit, long l1, TimeUnit timeUnit1) {
        return null;
    }

    @Override
    public CompletableFuture<Void> clearAsync() {
        return null;
    }

    @Override
    public CompletableFuture putIfAbsentAsync(Object o, Object o2) {
        return null;
    }

    @Override
    public CompletableFuture putIfAbsentAsync(Object o, Object o2, long l, TimeUnit timeUnit) {
        return null;
    }

    @Override
    public CompletableFuture putIfAbsentAsync(Object o, Object o2, long l, TimeUnit timeUnit, long l1, TimeUnit timeUnit1) {
        return null;
    }

    @Override
    public CompletableFuture removeAsync(Object o) {
        return null;
    }

    @Override
    public CompletableFuture<Boolean> removeAsync(Object o, Object o1) {
        return null;
    }

    @Override
    public CompletableFuture replaceAsync(Object o, Object o2) {
        return null;
    }

    @Override
    public CompletableFuture replaceAsync(Object o, Object o2, long l, TimeUnit timeUnit) {
        return null;
    }

    @Override
    public CompletableFuture replaceAsync(Object o, Object o2, long l, TimeUnit timeUnit, long l1, TimeUnit timeUnit1) {
        return null;
    }

    @Override
    public CompletableFuture<Boolean> replaceAsync(Object o, Object o2, Object v1) {
        return null;
    }

    @Override
    public CompletableFuture<Boolean> replaceAsync(Object o, Object o2, Object v1, long l, TimeUnit timeUnit) {
        return null;
    }

    @Override
    public CompletableFuture<Boolean> replaceAsync(Object o, Object o2, Object v1, long l, TimeUnit timeUnit, long l1, TimeUnit timeUnit1) {
        return null;
    }

    @Override
    public CompletableFuture getAsync(Object o) {
        return null;
    }

    @Override
    public void start() {

    }

    @Override
    public void stop() {

    }
}
