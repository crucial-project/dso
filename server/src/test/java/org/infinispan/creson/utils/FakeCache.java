package org.infinispan.creson.utils;

import org.infinispan.commons.api.BasicCache;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * @author Pierre Sutra
 */
public class FakeCache<K, V> implements BasicCache<K, V> {

   @Override public String getName() {
      return "fake";  // TODO: Customise this generated block
   }

   @Override public String getVersion() {
      return null;  // TODO: Customise this generated block
   }

   @Override public int size() {
      return 0;  // TODO: Customise this generated block
   }

   @Override public boolean isEmpty() {
      return false;  // TODO: Customise this generated block
   }

   @Override public boolean containsKey(Object key) {
      return false;  // TODO: Customise this generated block
   }

   @Override public boolean containsValue(Object value) {
      return false;  // TODO: Customise this generated block
   }

   @Override public V get(Object key) {
      return null;  // TODO: Customise this generated block
   }

   @Override public V put(K key, V value) {
      return null;  // TODO: Customise this generated block
   }

   @Override public V put(K key, V value, long lifespan, TimeUnit unit) {
      return null;  // TODO: Customise this generated block
   }

   @Override public V putIfAbsent(K key, V value, long lifespan, TimeUnit unit) {
      return null;  // TODO: Customise this generated block
   }

   @Override public void putAll(Map<? extends K, ? extends V> map, long lifespan, TimeUnit unit) {
      // TODO: Customise this generated block
   }

   @Override public V replace(K key, V value, long lifespan, TimeUnit unit) {
      return null;  // TODO: Customise this generated block
   }

   @Override public boolean replace(K key, V oldValue, V value, long lifespan, TimeUnit unit) {
      return false;  // TODO: Customise this generated block
   }

   @Override public V put(K key, V value, long lifespan, TimeUnit lifespanUnit, long maxIdleTime,
         TimeUnit maxIdleTimeUnit) {
      return null;  // TODO: Customise this generated block
   }

   @Override public V putIfAbsent(K key, V value, long lifespan, TimeUnit lifespanUnit, long maxIdleTime,
         TimeUnit maxIdleTimeUnit) {
      return null;  // TODO: Customise this generated block
   }

   @Override public void putAll(Map<? extends K, ? extends V> map, long lifespan, TimeUnit lifespanUnit,
         long maxIdleTime, TimeUnit maxIdleTimeUnit) {
      // TODO: Customise this generated block
   }

   @Override public V replace(K key, V value, long lifespan, TimeUnit lifespanUnit, long maxIdleTime,
         TimeUnit maxIdleTimeUnit) {
      return null;  // TODO: Customise this generated block
   }

   @Override public boolean replace(K key, V oldValue, V value, long lifespan, TimeUnit lifespanUnit,
         long maxIdleTime,
         TimeUnit maxIdleTimeUnit) {
      return false;  // TODO: Customise this generated block
   }

   @Override public V remove(Object key) {
      return null;  // TODO: Customise this generated block
   }

   @Override public void putAll(Map<? extends K, ? extends V> m) {
      // TODO: Customise this generated block
   }

   @Override public void clear() {
      // TODO: Customise this generated block
   }

   @Override public Set<K> keySet() {
      return null;  // TODO: Customise this generated block
   }

   @Override public Collection<V> values() {
      return null;  // TODO: Customise this generated block
   }

   @Override public Set<Entry<K, V>> entrySet() {
      return null;  // TODO: Customise this generated block
   }

   @Override public V putIfAbsent(K key, V value) {
      return null;  // TODO: Customise this generated block
   }

   @Override public boolean remove(Object key, Object value) {
      return false;  // TODO: Customise this generated block
   }

   @Override public boolean replace(K key, V oldValue, V newValue) {
      return false;  // TODO: Customise this generated block
   }

   @Override public V replace(K key, V value) {
      return null;  // TODO: Customise this generated block
   }

   @Override public void start() {
      // TODO: Customise this generated block
   }

   @Override public void stop() {
      // TODO: Customise this generated block
   }

   @Override
   public CompletableFuture<V> putAsync(K k, V v) {
      return null;
   }

   @Override
   public CompletableFuture<V> putAsync(K k, V v, long l, TimeUnit timeUnit) {
      return null;
   }

   @Override
   public CompletableFuture<V> putAsync(K k, V v, long l, TimeUnit timeUnit, long l1, TimeUnit timeUnit1) {
      return null;
   }

   @Override
   public CompletableFuture<Void> putAllAsync(Map<? extends K, ? extends V> map) {
      return null;
   }

   @Override
   public CompletableFuture<Void> putAllAsync(Map<? extends K, ? extends V> map, long l, TimeUnit timeUnit) {
      return null;
   }

   @Override
   public CompletableFuture<Void> putAllAsync(Map<? extends K, ? extends V> map, long l, TimeUnit timeUnit, long l1, TimeUnit timeUnit1) {
      return null;
   }

   @Override
   public CompletableFuture<Void> clearAsync() {
      return null;
   }

   @Override
   public CompletableFuture<V> putIfAbsentAsync(K k, V v) {
      return null;
   }

   @Override
   public CompletableFuture<V> putIfAbsentAsync(K k, V v, long l, TimeUnit timeUnit) {
      return null;
   }

   @Override
   public CompletableFuture<V> putIfAbsentAsync(K k, V v, long l, TimeUnit timeUnit, long l1, TimeUnit timeUnit1) {
      return null;
   }

   @Override
   public CompletableFuture<V> removeAsync(Object o) {
      return null;
   }

   @Override
   public CompletableFuture<Boolean> removeAsync(Object o, Object o1) {
      return null;
   }

   @Override
   public CompletableFuture<V> replaceAsync(K k, V v) {
      return null;
   }

   @Override
   public CompletableFuture<V> replaceAsync(K k, V v, long l, TimeUnit timeUnit) {
      return null;
   }

   @Override
   public CompletableFuture<V> replaceAsync(K k, V v, long l, TimeUnit timeUnit, long l1, TimeUnit timeUnit1) {
      return null;
   }

   @Override
   public CompletableFuture<Boolean> replaceAsync(K k, V v, V v1) {
      return null;
   }

   @Override
   public CompletableFuture<Boolean> replaceAsync(K k, V v, V v1, long l, TimeUnit timeUnit) {
      return null;
   }

   @Override
   public CompletableFuture<Boolean> replaceAsync(K k, V v, V v1, long l, TimeUnit timeUnit, long l1, TimeUnit timeUnit1) {
      return null;
   }

   @Override
   public CompletableFuture<V> getAsync(K k) {
      return null;
   }
}
