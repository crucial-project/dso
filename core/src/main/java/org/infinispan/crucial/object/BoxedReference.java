package org.infinispan.crucial.object;

import org.infinispan.crucial.utils.ContextManager;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.ObjectStreamException;

public class BoxedReference implements Externalizable{

    private Reference reference;

    public BoxedReference(){}

    public BoxedReference(Reference reference){
        this.reference = reference;
    }

    @Override
    public void writeExternal(ObjectOutput objectOutput) throws IOException {
        objectOutput.writeObject(this.reference);
    }

    @Override
    public void readExternal(ObjectInput objectInput) throws IOException, ClassNotFoundException {
        reference = (Reference) objectInput.readObject();
    }

    public Object readResolve() throws ObjectStreamException {
        return ContextManager.get().getFactory().getInstanceOf(reference);
    }

}
