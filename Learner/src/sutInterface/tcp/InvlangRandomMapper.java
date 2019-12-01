package sutInterface.tcp;

import invlang.types.EnumValue;
import invlang.types.FlagSet;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Random;

import sutInterface.Serializer;
import util.RangePicker;
import util.Tuple2;

public class InvlangRandomMapper extends InvlangMapper {
	private static final int RANDOM_ATTEMPTS = 3;
	private final Random random = new Random();
	private LinkedList<Long> valuesOfInterest = new LinkedList<>();
	public InvlangRandomMapper(File file) throws IOException {
		super(file);
	}

	public InvlangRandomMapper(String mapperName) throws IOException {
		super(mapperName);
	}
	
	@Override
	public String processOutgoingReset() {
		this.valuesOfInterest.clear();
		this.valuesOfInterest.add(0L);
		return super.processOutgoingReset();
	}
	
	@Override
	public String processOutgoingRequest(FlagSet flags, String absSeq,
			String absAck, int payloadLength) {
		for (Entry<String, Object> entry : this.handler.getState().entrySet()) {
			if (entry.getValue() instanceof Integer) {
				int value = (Integer) entry.getValue();
				if (value != InvlangMapper.NOT_SET && !this.valuesOfInterest.contains((long)value)) {
					this.valuesOfInterest.add(InvlangMapper.getUnsignedInt(value));
				}
			}
		}

		RangePicker picker = new RangePicker(random, 0, 0xffffffffL, this.valuesOfInterest);
		System.out.println("points of interest: " + this.valuesOfInterest);
		List<Integer> seqIndices = new ArrayList<>(), ackIndices = new ArrayList<>();
		int zeroIndex = -1;
		for (int i = 0; i < picker.getNumberOfRanges(); i++) {
			// zero is a special value, put it at the end
			if (picker.valueIsInRangeOfInterest(0L, i)) {
				zeroIndex = i;
			} else {
				seqIndices.add(i);
				ackIndices.add(i);
			}
		}
		Collections.shuffle(seqIndices);
		Collections.shuffle(ackIndices);
		List<Tuple2<Integer, Integer>> indicesOfInterest = new ArrayList<>();
		for (int i : seqIndices) {
			for (int j : ackIndices) {
				indicesOfInterest.add(new Tuple2<>(i, j));
			}
		}
		if (zeroIndex != -1) {
			for (int i : seqIndices) {
				indicesOfInterest.add(new Tuple2<>(i, zeroIndex));
			}
			for (int i : ackIndices) {
				indicesOfInterest.add(new Tuple2<>(zeroIndex, i));
			}
			indicesOfInterest.add(new Tuple2<>(zeroIndex, zeroIndex));
		}
		for (Tuple2<Integer, Integer> indexOfInterest : indicesOfInterest) {
			int i = indexOfInterest.tuple0, j = indexOfInterest.tuple1;
			int concSeq = (int) picker.getRandom(i), concAck = (int) picker.getRandom(j);
			handler.setFlags(Outputs.FLAGS_OUT, flags);
			handler.setInt(Outputs.CONC_SEQ, concSeq);
			handler.setInt(Outputs.CONC_ACK, concAck);
			handler.setInt(Outputs.CONC_DATA, payloadLength);
			handler.execute(Mappings.OUTGOING_REQUEST, false);
			EnumValue resultingAbsSeq = handler.getEnumResult(Outputs.ABS_SEQ);
			EnumValue resultingAbsAck = handler.getEnumResult(Outputs.ABS_ACK);
			if (resultingAbsSeq.getValue().equals(Validity.getValidity(absSeq).toInvLang())
					&& resultingAbsAck.getValue().equals(Validity.getValidity(absAck).toInvLang())) {
				handler.setFlags(Outputs.FLAGS_OUT, flags);
				handler.setInt(Outputs.CONC_SEQ, concSeq);
				handler.setInt(Outputs.CONC_ACK, concAck);
				handler.setInt(Outputs.CONC_DATA, payloadLength);
				handler.execute(Mappings.OUTGOING_REQUEST);
				long lConcSeq = getUnsignedInt(concSeq), lConcAck = getUnsignedInt(concAck);
				return Serializer.concreteMessageToString(flags, lConcSeq, lConcAck, payloadLength);
			}
		}
		throw new RuntimeException("Could not find concrete input");
		//return super.processOutgoingRequest(flags, absSeq, absAck, payloadLength);
	}
}
