package sutInterface.tcp;

import invlang.types.EnumValue;
import invlang.types.FlagSet;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;

import sutInterface.Serializer;
import util.Calculator;
import util.Log;
import util.exceptions.BugException;

public class WinInvlangRandomMapper extends InvlangMapper {
    
    public WinInvlangRandomMapper(File file) throws IOException {
        super(file);
    }

    public WinInvlangRandomMapper(String mapperName) throws IOException {
        super(mapperName);
    }
    
    @Override
    public String processOutgoingReset() {
        return super.processOutgoingReset();
    }
    
    public String processOutgoingRequest(FlagSet flags, String absSeq,
            String absAck, int payloadLength) {
        Integer lastLearnedSeqInt = (Integer) handler.getState().get("lastLearnerSeq");
        Integer concSeq;
        
        if (lastLearnedSeqInt == null || lastLearnedSeqInt == InvlangMapper.NOT_SET) {
            concSeq = (int) Calculator.randWithinRange(1000L, 0xffffL);
        } else {
            concSeq = (int) Calculator.sum(lastLearnedSeqInt, Calculator.randWithinRange(70000,100000));
        }
            
        Integer concAck = (int) Calculator.randWithinRange(1000L, 0xffffL);
        boolean isChecked = false;
        if (checkIfValidConcretization(flags, absSeq, concSeq, absAck, concAck, payloadLength)) {
            isChecked = true;
        } else {
            List<Long> pointsOfInterestLong = getPointsOfInterest();
            System.out.println(pointsOfInterestLong);
            List<Integer> pointsOfInterest = new ArrayList<Integer>();
            
            for (long num : pointsOfInterestLong) {
                pointsOfInterest.add((int) num);
            }
            //pointsOfInterest.add(0);
            for (Integer possibleAck : pointsOfInterest) {
                if (checkIfValidConcretization(flags, absSeq, concSeq, absAck, possibleAck, payloadLength)) {
                    concAck = possibleAck;
                    isChecked = true;
                    break;
                }
            }
            
            for (Integer possibleSeq : pointsOfInterest) {
                if (checkIfValidConcretization(flags, absSeq, possibleSeq, absAck, concAck, payloadLength)) {
                    concSeq = possibleSeq;
                    isChecked = true;
                    break;
                }
            }
            for (Integer possibleSeq : pointsOfInterest) {
                for (Integer possibleAck : pointsOfInterest) {
                    if (checkIfValidConcretization(flags, absSeq, possibleSeq, absAck, possibleAck, payloadLength)) {
                        concSeq = possibleSeq;
                        concAck = possibleAck;
                        isChecked = true;
                        break;
                    }
                }
            }
        }
        
        if (!isChecked) {
            
            throw new BugException("Cannot concretize the input for the windows mapper:\n" + handler.getState() + 
            		"\nhaving checked these points of interest " + getPointsOfInterest()
                    + "\n" + Arrays.asList(Thread.currentThread().getStackTrace()));
        } else {
            updateMapperWithConcretization(flags,concSeq,concAck, payloadLength);
            long lConcSeq = getUnsignedInt(concSeq), lConcAck = getUnsignedInt(concAck);
            return Serializer.concreteMessageToString(flags, lConcSeq, lConcAck, payloadLength);
        }
    }
    
    private List<Long> getPointsOfInterest() {
        List<Long> valuesOfInterest = new ArrayList<Long>();
//        valuesOfInterest.add(InvlangMapper.getUnsignedInt((Integer) (handler.getState().get("learnerSeq"))));
//        valuesOfInterest.add(InvlangMapper.getUnsignedInt((Integer) (handler.getState().get("learnerSeq")))+1);
//        valuesOfInterest.add(InvlangMapper.getUnsignedInt((Integer) (handler.getState().get("sutSeq"))));
//        valuesOfInterest.add(InvlangMapper.getUnsignedInt((Integer) (handler.getState().get("sutSeq")))+1);
//        valuesOfInterest.add(InvlangMapper.getUnsignedInt((Integer) (handler.getState().get("learnerSeqProposed"))));
//        valuesOfInterest.add(0L);
//        
        for (Entry<String, Object> entry : this.handler.getState().entrySet()) {
            if (entry.getValue() instanceof Integer) {
                int value = (Integer) entry.getValue();
                if (value != InvlangMapper.NOT_SET && !valuesOfInterest.contains((long)value)) {
                    long longOfVal = InvlangMapper.getUnsignedInt(value);
                    for (int i=0; i < 2; i ++) {
                        if (!valuesOfInterest.contains(longOfVal+i)) {
                            valuesOfInterest.add((long)(longOfVal+i));
                        }
                    }
                }
            }
        }
        valuesOfInterest.add(0L);
        
        return valuesOfInterest;
    }
    
    
    
    private boolean checkIfValidConcretization(FlagSet flags, String absSeq, int concSeq,
            String absAck, int concAck, int payloadLength) {
        handler.setFlags(Outputs.FLAGS_OUT, flags);
        handler.setInt(Outputs.CONC_SEQ, concSeq);
        handler.setInt(Outputs.CONC_ACK, concAck);
        handler.setInt(Outputs.CONC_DATA, payloadLength);
        handler.execute(Mappings.OUTGOING_REQUEST, false);
        EnumValue resultingAbsSeq = handler.getEnumResult(Outputs.ABS_SEQ);
        EnumValue resultingAbsAck = handler.getEnumResult(Outputs.ABS_ACK);
        return resultingAbsSeq.getValue().equals(Validity.getValidity(absSeq).toInvLang())
                && resultingAbsAck.getValue().equals(Validity.getValidity(absAck).toInvLang());
    }
    
    private void updateMapperWithConcretization(FlagSet flags, int concSeq, int concAck, int payloadLength) {
        handler.setFlags(Outputs.FLAGS_OUT, flags);
        handler.setInt(Outputs.CONC_SEQ, concSeq);
        handler.setInt(Outputs.CONC_ACK, concAck);
        handler.setInt(Outputs.CONC_DATA, payloadLength);
        handler.execute(Mappings.OUTGOING_REQUEST);
        
    }
    
}
