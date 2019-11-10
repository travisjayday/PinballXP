#include "jni/dosbox/include/inout.h"
static void write_p3c6(Bitu port,Bitu val,Bitu iolen) {
	if ( vga.dac.pel_mask != val ) {
		LOG(LOG_VGAMISC,LOG_NORMAL)("VGA:DCA:Pel Mask set to %X", val);
		vga.dac.pel_mask = val;
#if HAVE_NEON
		for ( Bitu i = 0;i<32;i++)
		{

			Bit8u red[8];
			Bit8u green[8];
			Bit8u blue[8];

			for(Bitu j = 0;j<8;j++)
			{
				 RGBEntry rgbe=vga.dac.rgb[(i*8+j)& val];
				 red[j]=  rgbe.red;
				 green[j]= rgbe.green;
				 blue[j]= rgbe.blue;
			}

			uint8x8_t red_v =vld1_u8(red);
			uint8x8_t green_v =vld1_u8(green);
			uint8x8_t blue_v =vld1_u8(blue);

			// ((blue>>1)&0x1f) | (((green)&0x3f)<<5) | (((red>>1)&0x1f) << 11);
			uint16x8_t xlat16_v =vld1q_u16(0);
			xlat16_v = vorrq_u16(xlat16_v,vshll_n_u8(vand_u8(vshr_n_u8(blue_v,1),vmov_n_u8((Bit8u)0x1f)),0));
			xlat16_v = vorrq_u16(xlat16_v,vshll_n_u8(vand_u8(green_v,vmov_n_u8((Bit8u)0x3f)),5));
			xlat16_v = vorrq_u16(xlat16_v,vshll_n_u8(vand_u8(vshr_n_u8(red_v,1),vmov_n_u8((Bit8u)0x1f)),11));

			uint8x8_t pal_red_v=vorr_u8(vshl_n_u8(red_v,2),vshr_n_u8(red_v,4));
			uint8x8_t pal_green_v=vorr_u8(vshl_n_u8(green_v,2),vshr_n_u8(green_v,4));
			uint8x8_t pal_blue_v=vorr_u8(vshl_n_u8(blue_v,2),vshr_n_u8(blue_v,4));

			Bit8u pal_red[8],pal_green[8],pal_blue[8];
			vld1q_lane_u16(&(vga.dac.xlat16[i*8]),xlat16_v,7);
			vld1_lane_u8(pal_red,pal_red_v,7);
			vld1_lane_u8(pal_green,pal_green_v,7);
			vld1_lane_u8(pal_blue,pal_blue_v,7);

			for(Bitu j = 0;j<8;j++)
			{
				RENDER_SetPal((i*8+j),pal_red[j],pal_green[j],pal_blue[j]);
			}
		}
#else
		for ( Bitu i = 0;i<256;i++) 
			VGA_DAC_UpdateColor( i );
#endif
	}
}

