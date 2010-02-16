# Each file in this directory describes the geophysical bands provided
# by each of the ENVISAT data products. A file contains a band
# description record in each line.
#
# The records have the following columns:
#
# 1) band_name:
#      The name as presented to the user in a band selection dialog
#      (also known as spectral subset)
#
# 2) dataset_name:
#      The name of the source dataset containing the raw data used to
#      create the band samples. The format is <MDS-name>.<field>, where
#      <field> is a one-based index (field=1 corresponds to the first field)
#
# 3) sample_model_op:
#      The sample model operation applied to the source dataset for getting the
#      correct samples from the MDS (e.g. MERIS L2). Possible values are
#      *  --> no operation (direct copy)
#      1OF2  --> first byte of 2-byte interleaved MDS
#      2OF2  --> second byte of 2-byte interleaved MDS
#      0123  --> combine 3-bytes interleaved to 4-byte integer
#
# 4) band_datatype:
#      The data type of the band's pixel values. Possible values are:
#      *      --> the datatype remains unchanged.
#      UChar  --> 8-bit unsigned integer
#      ULong  --> 32-bit unsigned integer
#      Float  --> 32-bit IEEE floating point
#
# 5) spectr_band_index
#      The spectral band index
#      *      --> not a spectral band
#      const. --> an integer, the first band is always 1
#
# 6) scaling_method:
#      The scaling method which must be applied to the raw source data in order
#      to get the 'real' pixel values in geo-physical units. Possible values are:
#      *            --> no scaling applied
#      Linear_Scale --> linear scaling applied: y = offset + scale * x
#      Log_Scale    --> logarithmic scaling applied: y = log(offset + scale * x)
#
# 7) scaling_offset:
#      The scaling offset. Possible values are:
#      *            --> no offset provided (implies scaling_method=*)
#      <const.>     --> a floating point constant
#      <GADS>.<field>[.<field2>]
#                   --> value is provided in global annotation dataset with name
#                       <GADS> in field <field>. Optionally a second element index
#                       for multiple-element fields can be given too
#
# 8) scaling_factor:
#      The scaling factor. Possible values are:
#      *            --> no factor provided (implies scaling_method=*)
#      const.       --> a floating point constant
#      <GADS>.<field>[.<field2>]
#                   --> value is provided in global annotation dataset with name
#                       <GADS> in field <field>. Optionally a second element index
#                       for multiple-element fields can be given too
#
# 9) bit_mask:
#      A bit-mask expression used to filter valid pixels. All others are set to zero.
#
# 10) flags_definition_file:
#      A reference to the file containing all available flags contained in a sample of a
#      flag dataset. Valid for flags datasets (UChar, UShort, ULong) only, set to
#      * otherwise
#
# 11) unit
#      The geophysical unit for the band's pixel values
#
# 12) description
#      A short description of the band's contents
#
