package org.crucial.dso.object;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;


/**
 * @author Pierre Sutra
 *
 * The ID of a response is the one of the call it is answering.
 *
 */
public class CallResponse extends Call {

    private Object result;
    private Object state;

    @Deprecated
    public CallResponse() {
    }

    public CallResponse(Reference reference, Call call) {
        super(reference, call);
        this.result = null;
    }

    public void setResult(Object result) {
        this.result = result;
    }

    public Object getResult() {
        return this.result;
    }

    public Object getState() {
        return state;
    }

    public void setState(Object state) {
        this.state = state;
    }

    @Override
    public String toString() {
        return super.toString()+"-RESP-("+getCallID()+")" + result + "]";
    }

    @Override
    public void writeExternal(ObjectOutput objectOutput) throws IOException {
        super.writeExternal(objectOutput);
        objectOutput.writeObject(state);
        objectOutput.writeObject(result);
    }

    @Override
    public void readExternal(ObjectInput objectInput) throws IOException, ClassNotFoundException {
        super.readExternal(objectInput);
        state = objectInput.readObject();
        result = objectInput.readObject();
    }
}
