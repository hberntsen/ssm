/**
 * Simple Stack Machine
 *
 * Written by Atze Dijkstra, atze@cs.uu.nl,
 * Copyright Utrecht University.
 *
 */

package nl.uu.cs.ssm ;

import java.util.Enumeration;

public class MachineState extends Model
{
    protected int           stackBottom     ;
    protected int           stackGrowthDir  ;
    
    private final int startAddressOfHeap;
    
    protected int           code            ;
    protected int           instrPC         ;
    protected Instruction   instr           ;
    protected int           nInlineOpnds    ;
    protected int           inlineOpnds[]   ;
    protected Memory		memory			;
    protected Registers		registers		;
    protected MemoryUser    memoryUser      ;
    
    public    boolean       isHalted        ;
    
    public MachineState( int initMemCapacity, int startAddressOfHeap, Messenger m )
    {
    	memory = new Memory( initMemCapacity, m ) ;
    	registers = new Registers( memory, m ) ;
    	stackGrowthDir = 1 ;
    	this.startAddressOfHeap = startAddressOfHeap;
    	reset() ;
    }
    
    protected void fireStateChange( MachineStateEvent ev )
    {
    	for( Enumeration e = getListeners() ; e.hasMoreElements() ; )
    	{
    		MachineStateListener l = (MachineStateListener)e.nextElement() ;
    		l.stateChanged( ev ) ;
    	}
    }

    public void addMachineStateListener( MachineStateListener l )
    {
    	addListener( l ) ;
    }
    
    public void removeMachineStateListener( MachineStateListener l )
    {
    	removeListener( l ) ;
    }
    
    public void reset( )
    {
		memory.reset() ;
		registers.reset() ;
		resetToInitialState() ;
    }
    
    public void resetToInitialState()
    {
        registers.setPC( 0 ) ;
		stackBottom = memory.getUsedForCode() + 16 ;
        registers.setSP( stackBottom - stackGrowthDir ) ;
        registers.setMP( registers.getSP() ) ;
        registers.setHP(startAddressOfHeap);
        isHalted = false ;
    }
    
    public int dir( int v )
    {
    	return v * stackGrowthDir ;
    }
    
    public void setCurrentInstr( int pc, int code, Instruction instr )
    {
        this.code = code ;
        instrPC = pc ;
        this.instr = instr ;
        nInlineOpnds = instr.getNrInlineOpnds() ;
        if ( nInlineOpnds > 0 )
            inlineOpnds = new int[ nInlineOpnds ] ;
    }
    
    public Memory getMemory()
    {
    	return memory ;
    }
    
    public int getStackBottom()
    {
    	return stackBottom ;
    }
    
    public int getStartAddressOfHeap() {
    	
    	return startAddressOfHeap;
    }
        
    public Registers getRegisters()
    {
    	return registers ;
    }
    
    public boolean stackIsEmpty()
    {
        return stackBottom == registers.getSP() ;
    }
    
    public int stackTop()
    {
        return registers.getRegInd( Registers.SP ) ;
    }
    
    public int getSR( )
    {
        return stackIsEmpty() ? 0 : stackTop() ;
    }
    
    public String getSRAsString( )
    {
        int sr = getSR() ;
        return   "Z=" + ((sr & Instruction.ZZ) >> Instruction.SR_Z) +
                " C=" + ((sr & Instruction.CC) >> Instruction.SR_C) +
                " V=" + ((sr & Instruction.VV) >> Instruction.SR_V) +
                " N=" + ((sr & Instruction.NN) >> Instruction.SR_N) ;
    }
    
    class UndoStateModification implements Modification
    {
    	private boolean wasHalted ;
    	
    	UndoStateModification( boolean h )
    	{
    		wasHalted = h ;
    	}
    	
    	public void modify()
    	{
    		isHalted = wasHalted ;
    	}
    }
    
    public void setHalted()
    {
        isHalted = true ;
        fireStateChange( new MachineStateEvent( this, new UndoStateModification( false ) ) ) ;
    }
    
    public boolean isHalted()
    {
        return isHalted ;
    }
    
    public String toString()
    {
        return "state code=" + code + " instr-pc=" + instrPC + " n-inl=" + nInlineOpnds ;
    }
    
    
}