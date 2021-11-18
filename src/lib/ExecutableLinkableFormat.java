package lib;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.util.Arrays;

public class ExecutableLinkableFormat {
	
	public static byte[] saveToFile(int[] programData) throws IOException {
		ByteBuffer dest = ByteBuffer.allocate(programData.length * 4);
		dest.asIntBuffer().put(programData);

		byte[] fh = FileHeader.fileHeader;
		byte[] ph = ProgramHeader.programHeader;
		byte[] pd = dest.array();
		byte[] absoluteFileBytes = new byte[fh.length + ph.length + dest.capacity()];
		
		System.arraycopy(fh, 0, absoluteFileBytes, 0, fh.length);
		System.arraycopy(ph, 0, absoluteFileBytes, fh.length, ph.length);
		System.arraycopy(pd, 0, absoluteFileBytes, fh.length + ph.length, pd.length);
		
		ByteBuffer.wrap(absoluteFileBytes).putInt(FileHeader.e_entryOffset, 0); // e_entry
		ByteBuffer.wrap(absoluteFileBytes).putInt(fh.length + ProgramHeader.p_fileszOffset, pd.length); // p_filesz
		ByteBuffer.wrap(absoluteFileBytes).putInt(fh.length + ProgramHeader.p_memszOffset, pd.length); // p_memsz
		
		return absoluteFileBytes;
	}
	
	public static byte[] loadProgramData(File file, Integer rIP, Integer memoffs) throws IOException {
		byte[] absoluteFileBytes = Files.readAllBytes(file.toPath());
		return loadProgramData(absoluteFileBytes, rIP, memoffs);
	}
	public static byte[] loadProgramData(byte[] absoluteFileBytes, Integer rIP, Integer memoffs) throws IOException {
		int len = ByteBuffer.wrap(absoluteFileBytes)
				.getInt(FileHeader.fileHeader.length + ProgramHeader.p_fileszOffset); // p_filesz
		byte[] fh = new byte[FileHeader.fileHeader.length];
		byte[] ph = new byte[ProgramHeader.programHeader.length];
		byte[] pd = new byte[len];

		System.arraycopy(absoluteFileBytes, 0, fh, 0, fh.length);
		System.arraycopy(absoluteFileBytes, fh.length, ph, 0, ph.length);
		System.arraycopy(absoluteFileBytes, fh.length + ph.length, pd, 0, len);
		
		
		rIP = ByteBuffer.wrap(fh).getInt(FileHeader.e_entryOffset);  // e_entry
		memoffs = ByteBuffer.wrap(ph).getInt(ProgramHeader.p_vaddrOffset);  // p_vaddr
		ByteBuffer.wrap(fh).putInt(FileHeader.e_entryOffset, 0);
		
		if(!Arrays.equals(fh, FileHeader.fileHeader))
			throw new RuntimeException("Incorrect file format");
		
		return pd;
	}
	
//	public static void printFormat() { }
	
	public static class FileHeader{
		
		int e_entry;
		short e_shnum;
		short e_shstrndx;
		
		final static int e_entryOffset = 24;
		
		final static byte[] fileHeader = {
				0x7f, 0x45, 0x4c, 0x46,	//EI_MAG0-3		.ELF
				0x01,					//EI_CLASS		32-bit version
				0x01,					//EI_DATA		Little endian
				0x01,					//EI_VERSION	Original version of ELF
				0x00, 0x00,				//EI_OSABI		No target operating system ABI
				0, 0, 0, 0, 0, 0, 0,	//EI_PAD		ignored
				
				0x02, 0x00,				//e_type		ET_EXEC object file type
				0x33, 0x00, 			//e_machine		RISC160016CPU32B1Vx instruction set
				0x01, 0x00, 0x00, 0x00,	//e_version		Original version of ELF
				0, 0, 0, 0,				//e_entry		*Initialize with rIP (instruction pointer register) at
				0x34, 0x00, 0x00, 0x00,	//e_phoff		Program header offset
				0x54, 0x00, 0x00, 0x00,	//e_shoff		Section header offset
				0, 0, 0, 0,				//e_flags		ignored
				0x34, 0x00, 			//e_ehsize		File header size
				0x20, 0x00, 			//e_phentsize	Program header entry size
				0x01, 0x00, 			//e_phnum		Program header amount
				0x28, 0x00, 			//e_shentsize	Section header entry size
				0, 0, 					//e_shnum		*Section header amount
				0, 0, 					//e_shstrndx	*Section header index containing the names
		};
		
		String[] fileHeaderStrings = {
			"4	EI_MAG0-3",
			"1	EI_CLASS"
			
		};
	}
	
	static class ProgramHeader {
		
		int p_filesz;
		int p_memsz;
		
		
		final static int p_fileszOffset = 20;
		final static int p_memszOffset = 24;
		final static int p_vaddrOffset = 8;
		
		final static byte[] programHeader = {
				0x01, 0x00, 0x00, 0x00,	//p_type		Program segment will be loaded
				0x00, 0x00, 0x00, 0x00,	//p_offset		Offset of the segment in the file image
				0x00, 0x00, 0x00, 0x00,	//p_vaddr		Virtual starting address to load into
				0, 0, 0, 0,				//p_paddr		ignored
				0, 0, 0, 0,				//p_filesz		*Load size in file
				0, 0, 0, 0,				//p_memsz		*Load size in memory
				0, 0, 0, 0,				//p_flags		ignored
				0x40, 0x00, 0x00, 0x00,	//p_align		32 bit alignment of memory 
				
		};
	}
	
	class SectionHeader {
		
		int sh_name;
		int sh_type;
		int sh_flags;
		int sh_addr;
		int sh_offs;
		int sh_size;
		
		byte[] sectionHeader = {
				0, 0, 0, 0,				//sh_name		*Offset of string in .shstrtab section
				0, 0, 0, 0,				//sh_type		*Section type
				0, 0, 0, 0,				//sh_flags		*Section attributes
				0, 0, 0, 0,				//sh_addr		*Load address in memory
				0, 0, 0, 0,				//sh_offs		*Load offset in file
				0, 0, 0, 0,				//sh_size		*Size in file
				0, 0, 0, 0,				//sh_link		ignored
				0, 0, 0, 0,				//sh_info		ignored
				0x40, 0x00, 0x00, 0x00,	//sh_addralign	32 bit alignment of memory address
				0, 0, 0, 0,				//sh_entsize	ignored
				
		};
	}

	

}
