package org.infinispan.creson;

import javax.persistence.Entity;
import javax.persistence.Id;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

@Entity
public class AtomicMatrix<T> {

    @Id
    public String id;

    public T[][] array;

    public AtomicMatrix(){}

    public AtomicMatrix(String id, Class<T> clazz, int n, int m) {
        this.id = id;
        this.array = (T[][]) Array.newInstance(clazz, n, m);
    }

    public AtomicMatrix(String id, Class<T> clazz, T zero, int n, int m) {
        this.id = id;
        this.array = (T[][]) Array.newInstance(clazz, n, m);
        for (int i=0; i<array.length; i++) {
            for (int j = 0; j < array[i].length; j++) {
                array[i][j] = zero;
            }
        }
    }

    public AtomicMatrix(String id, T[][] array) {
        this.id = id;
        this.array = array;
    }

    public int rows(){
        return array.length;
    }

    public int columns(){
        return array[0].length;
    }

    public T get(int i, int j){
        return array[i][j];
    }

    public T[] get(int i) {
        return array[i];
    }

    public void compute(int i, int j, BiFunction<T,T,T> f, T v){
        array[i][j] = f.apply(array[i][j],v);
    }

    public void forEach(Function<T,T> f){
        for (int i=0; i<array.length; i++) {
            for (int j=0; j<array[i].length; j++){
                array[i][j] = f.apply(array[i][j]);
            }
        }
    }

    public void compute(T[][] array, BiFunction<T,T,T> f){
        for (int i=0; i<array.length; i++) {
            for (int j=0; j<array[i].length; j++){
                this.array[i][j] = f.apply(array[i][j], this.array[i][j]);
            }
        }
    }

    public T[][] toArray(){
        return array;
    }
}
