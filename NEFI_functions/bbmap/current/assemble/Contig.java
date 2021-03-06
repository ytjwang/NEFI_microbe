package assemble;

import dna.AminoAcid;
import structures.ByteBuilder;
import ukmer.Kmer;

/**
 * Contig generated by Tadpole.
 * @author Brian Bushnell
 * @date July 12, 2018
 *
 */
public class Contig {
	
	public Contig(byte[] bases_){
		this(bases_, null, 0);
	}
	
	public Contig(byte[] bases_, int id_){
		this(bases_, null, id_);
	}
	
	public Contig(byte[] bases_, String name_, int id_){
		bases=bases_;
		name=name_;
		id=id_;
	}
	
	public ByteBuilder toFasta(int wrap, ByteBuilder sb){
		if(wrap<1){wrap=Integer.MAX_VALUE;}
		sb.append('>');
		toHeader(sb);
		if(bases!=null){
			int pos=0;
			while(pos<bases.length-wrap){
				sb.append('\n');
				sb.append(bases, pos, wrap);
				pos+=wrap;
			}
			if(pos<bases.length){
				sb.append('\n');
				sb.append(bases, pos, bases.length-pos);
			}
		}
		return sb;
	}
	
	private ByteBuilder toHeader(ByteBuilder sb){
		if(name!=null){return sb.append(name);}
		sb.append("contig_").append(id);
		sb.append(",length=").append(length());
		sb.append(",cov=").append(coverage, 1);
		sb.append(",min=").append(minCov);
		sb.append(",max=").append(maxCov);
		sb.append(",gc=").append(gc(), 3);
		sb.append(",left=").append(Tadpole.codeStrings[leftCode]);
		if(leftCode==Tadpole.F_BRANCH || leftCode==Tadpole.B_BRANCH){sb.append('_').append(leftRatio, 1);}
		sb.append(",right=").append(Tadpole.codeStrings[rightCode]);
		if(rightCode==Tadpole.F_BRANCH || rightCode==Tadpole.B_BRANCH){sb.append('_').append(rightRatio, 1);}
		return sb;
	}

	/**
	 * @return GC fraction
	 */
	public float gc() {
		if(bases==null || bases.length<1){return 0;}
		int at=0, gc=0;
		for(byte b : bases){
			int x=AminoAcid.baseToNumber[b];
			if(x>-1){
				if(x==0 || x==3){at++;}
				else{gc++;}
			}
		}
		if(gc<1){return 0;}
		return gc*1f/(at+gc);
	}
	
	public int length(){return bases.length;}
	
	long leftKmer(int k){
		assert(k<32);
		long kmer=0;
		for(int i=0; i<k; i++){
			byte b=bases[i];
			byte x=AminoAcid.baseToNumber[b];
			assert(x>=0);
			kmer=(kmer<<2)|x;
		}
		return kmer;
	}
	
	long rightKmer(int k){
		assert(k<32);
		long kmer=0;
		for(int i=bases.length-k; i<bases.length; i++){
			byte b=bases[i];
			byte x=AminoAcid.baseToNumber[b];
			assert(x>=0);
			kmer=(kmer<<2)|x;
		}
		return kmer;
	}
	
	Kmer leftKmer(Kmer kmer){
		kmer.clearFast();
		for(int i=0; i<kmer.kbig; i++){
			kmer.addRight(bases[i]);
		}
		return kmer;
	}
	
	Kmer rightKmer(Kmer kmer){
		kmer.clearFast();
		for(int i=bases.length-kmer.kbig; i<bases.length; i++){
			kmer.addRight(bases[i]);
		}
		return kmer;
	}
	
	boolean canonical(){
		for(int i=0, j=bases.length-1; i<bases.length; i++, j--){
			final byte a=AminoAcid.baseToComplementExtended[i], b=bases[j];
			if(a<b){return true;}
			else if(b<a){return false;}
		}
		return true;
//		final long leftKmer=leftKmer(k);
//		final long rightKmer=rightKmer(k);
//		final long leftRKmer=AminoAcid.reverseComplementBinaryFast(leftKmer, k);
//		assert((rightKmer!=leftKmer && rightKmer!=leftRKmer) || length()==k /* Palindrome case */);
//		return rightKmer>=leftRKmer;
	}
	
	void rcomp(){
		AminoAcid.reverseComplementBasesInPlace(bases);
		{
			int temp=leftCode;
			leftCode=rightCode;
			rightCode=temp;
		}
		{
			float temp=leftRatio;
			leftRatio=rightRatio;
			rightRatio=temp;
		}
		{
			Edge[] temp=leftEdges;
			leftEdges=rightEdges;
			rightEdges=temp;
		}
	}
	
	boolean leftBranch(){return Tadpole.isBranchCode(leftCode);}
	boolean rightBranch(){return Tadpole.isBranchCode(rightCode);}
	
	public String name;
	public byte[] bases;
	public float coverage;
	public int minCov;
	public int maxCov;
	int leftCode;
	int rightCode;
	float leftRatio;
	float rightRatio;
	public int id;
	
	public Edge[] leftEdges;
	public Edge[] rightEdges;
}
