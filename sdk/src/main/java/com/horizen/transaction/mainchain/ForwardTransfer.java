package com.horizen.transaction.mainchain;

import com.google.common.primitives.Bytes;
import com.google.common.primitives.Ints;
import com.horizen.block.MainchainTxForwardTransferCrosschainOutput;
import com.horizen.block.MainchainTxSidechainCreationCrosschainOutput;
import com.horizen.box.RegularBox;
import com.horizen.proposition.PublicKey25519Proposition;
import com.horizen.utils.BytesUtils;
import com.horizen.utils.Utils;
import scorex.crypto.hash.Blake2b256;

import java.util.Arrays;
import java.util.Optional;

public final class ForwardTransfer implements SidechainRelatedMainchainOutput<RegularBox> {

    private MainchainTxForwardTransferCrosschainOutput output;
    private byte[] containingTxHash;
    private int index;

    public ForwardTransfer(MainchainTxForwardTransferCrosschainOutput output, byte[] containingTxHash, int index) {
        this.output = output;
        this.containingTxHash = containingTxHash;
        this.index = index;
    }
    @Override
    public byte[] hash() {
        return BytesUtils.reverseBytes(Utils.doubleSHA256Hash(Bytes.concat(
                BytesUtils.reverseBytes(output.hash()),
                BytesUtils.reverseBytes(containingTxHash),
                BytesUtils.reverseBytes(Ints.toByteArray(index))
        )));
    }

    @Override
    public Optional<RegularBox> getBox() {
        byte[] hash = Blake2b256.hash(Bytes.concat(containingTxHash, Ints.toByteArray(index)));
        long nonce = BytesUtils.getLong(hash, 0);
        return Optional.of(new RegularBox(new PublicKey25519Proposition(output.propositionBytes()), nonce, output.amount()));
    }

    @Override
    public byte[] bytes() {
        return Bytes.concat(
                output.forwardTransferOutputBytes(),
                containingTxHash,
                Ints.toByteArray(index)
        );
    }

    public static ForwardTransfer parseBytes(byte[] bytes) {
        if(bytes.length < 36 + MainchainTxForwardTransferCrosschainOutput.FORWARD_TRANSFER_OUTPUT_SIZE())
            throw new IllegalArgumentException("Input data corrupted.");

        int offset = 0;

        MainchainTxForwardTransferCrosschainOutput output = MainchainTxForwardTransferCrosschainOutput.create(bytes, offset).get();
        offset += MainchainTxForwardTransferCrosschainOutput.FORWARD_TRANSFER_OUTPUT_SIZE();

        byte[] txHash = Arrays.copyOfRange(bytes, offset, offset + 32);
        offset += 32;

        int idx = BytesUtils.getInt(bytes, offset);

        return new ForwardTransfer(output, txHash, idx);
    }

    @Override
    public SidechainRelatedMainchainOutputSerializer serializer() {
        return ForwardTransferSerializer.getSerializer();
    }
}
