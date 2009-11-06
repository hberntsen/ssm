/** * Simple Stack Machine * * Written by Atze Dijkstra, atze@cs.uu.nl, * Copyright Utrecht University. * */package nl.uu.cs.ssm ;import java.util.* ;import java.awt.* ;public abstract class MetaInstruction{    private static Hashtable colorMap ;        static    {        colorMap = new Hashtable() ;        colorMap.put( "black"       , Color.black       ) ;        colorMap.put( "blue"        , Color.blue        ) ;        colorMap.put( "cyan"        , Color.cyan        ) ;        colorMap.put( "darkGray"    , Color.darkGray    ) ;        colorMap.put( "gray"        , Color.gray        ) ;        colorMap.put( "green"       , Color.green       ) ;        colorMap.put( "lightGray"   , Color.lightGray   ) ;        colorMap.put( "magenta"     , Color.magenta     ) ;        colorMap.put( "orange"      , Color.orange      ) ;        colorMap.put( "pink"        , Color.pink        ) ;        colorMap.put( "red"         , Color.red         ) ;        colorMap.put( "yellow"      , Color.yellow      ) ;    }        protected Instruction instruction ;        private MetaInstruction( Instruction instr )    {        instruction = instr ;    }        public abstract void exec( MachineState machineState ) ;    private static class Annote extends MetaInstruction    {        int register ;        int loDispl, hiDispl ;        Color color ;        String text ;                Annote( Instruction i, int r, int l, int h, Color c, String t )        {            super( i ) ;            register = r ;            loDispl = l ;            hiDispl = h ;            color = c ;            text = t ;        }                public void exec( MachineState machineState )        {            Memory mem = machineState.getMemory() ;            Registers regs = machineState.getRegisters() ;            for ( int i = loDispl ; i <= hiDispl ; i++ )            {                mem.setAnnotationAt( regs.getRegDispl( register, i ), new MemoryAnnotation( text, color ) ) ;            }        }                public String toString()        {        	return "Annote reg=" + register + " displ=" + loDispl + "/" + hiDispl + " col=" + color + " text=" + text ;        }    }        public static MetaInstruction newAnnote( Instruction instr, Vector args )    {        int r = Registers.findRegOfName( (String)args.elementAt(0) ) ;        int l = Utils.fromHex( (String)args.elementAt(1) ) ;        int h = Utils.fromHex( (String)args.elementAt(2) ) ;        Color c = (Color)colorMap.get( (String)args.elementAt(3) ) ;        String t = (String)args.elementAt(4) ;                if ( r >= 0 && l <= h && c != null && t != null )            return new Annote( instr, r, l, h, c, t ) ;        else            return null ;    }    }