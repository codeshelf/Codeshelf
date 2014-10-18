-- Codeshelf/Flyweight dissectors
-- FLyweight packets are 802.15.4 packets with an FCF of 0x7eff (Freescale SMAC)
-- They may come to Wireshark in two formats: encapsulated in ZEP packets or in raw 802.15.4 (wpan, wpan_nofcs) packets.
-- For this reason we have to intercept both kinds of encapsulation and chain the dissectors together to get the parsing done.
-- This allows us to digest the 802.15.4 packets in any format we want.

----------------------------------------------------------
-- ZEP protocol
zepproto = Proto("zepext","ZEP Protocol")
zepfields = zepproto.fields
zepfields.f_version = ProtoField.uint8("zepfw.version", "Ver", base.HEX)
zepfields.f_channel_id = ProtoField.uint8("zepfw.channel", "Channel", base.DEC)
zepfields.f_device_id = ProtoField.uint16("zepfw.deviceid", "DevID", base.DEC)
zepfields.f_lqi_mode = ProtoField.uint8("zepfw.lqi_mode", "LQI Mode", base.HEX)
zepfields.f_lqi = ProtoField.uint8("zepfw.lqi", "LQI", base.DEC)
zepfields.f_ntp_time = ProtoField.uint32("zepfw.version", "Time", base.HEX)
zepfields.f_seqnum = ProtoField.uint32("zepfw.seqnum", "SeqNum", base.DEC)
zepfields.f_ieee_packet_len = ProtoField.uint8("zepfw.wpan_len", "802.15.4 Packet Len", base.DEC)

-- ZEP dissector function
function zepproto.dissector(tvb, pkt, root)
  -- validate packet length is adequate, otherwise quit
  if tvb:len() == 0 then return end

  local f_preamble = tvb(0,2):string()

  -- If the preamble is not EX then it's not a ZEP packet and we'll just parse it out as data
  if f_preamble ~= "EX" then
    local data_dissector = Dissector.get("data")
    data_dissector:call(tvb, pkt, root)
    return
  end

  -- Protocol version
  zepfields.f_version = tvb(2,1):uint()
  -- Set the protocol column info
  pkt.cols.protocol = "ZEPv"..zepfields.f_version

  -- We're only supporting V2 ZEP (for now)
  if zepfields.f_version ~= 2 and zepfields.f_version ~= 3 then
    zep_dissector:call(tvb, pkt, root)
    return
  end

  -- create subtree for ZEP
  zepsubtree = root:add(zepproto, tvb(0))

  -- ZEP type
  local f_type = tvb(3,1):uint()

  if f_type == 2 then
  else
    zepsubtree:add_packet_field(zepfields.f_channel_id, tvb:range(4,1), ENC_UTF_8 + ENC_STRING)
    zepsubtree:add_packet_field(zepfields.f_device_id, tvb:range(5,2), ENC_BIG_ENDIAN)
    zepsubtree:add_packet_field(zepfields.f_lqi_mode, tvb:range(7,1), ENC_UTF_8 + ENC_STRING)
    zepsubtree:add_packet_field(zepfields.f_lqi, tvb:range(8,1), ENC_UTF_8 + ENC_STRING)
    zepsubtree:add_packet_field(zepfields.f_ntp_time, tvb:range(9,4), ENC_BIG_ENDIAN)
    zepsubtree:add_packet_field(zepfields.f_seqnum, tvb:range(17,4), ENC_BIG_ENDIAN)
    zepsubtree:add_packet_field(zepfields.f_ieee_packet_len, tvb:range(31,1), ENC_UTF_8 + ENC_STRING)

    local devid = tvb:range(7,1):uint()
    local seqnum = tvb:range(17,4):uint()
    pkt.cols.info = "Dev ID: "..devid.." Seq Num: "..seqnum

    local fcf = tvb:range(32,2):uint()

    if fcf == 0x7eff then
      -- Now dissect the Flyweight
      flyweightproto.dissector:call(tvb(32):tvb(), pkt, root)
    else
      -- Now dissect the 802.15.4 packet
      local wpan_range = tvb:range(32, tvb:len() - 32):bytes()
      local wpan_tvb = ByteArray.tvb(wpan_range, "WPan Tvb")
      wpan_dissector:call(tvb(32):tvb(), pkt, root)
    end
  end
end

-- Initialization routine
function zepproto.init()
end


----------------------------------------------------------
-- Flyweight protocol
local VER_BITS = {[0] = "1", [1] = "Ver 2"}
local TYPE_BITS = {[0] = "Std", [1] = "Ack"}
local NET_BITS = {[15] = "Broadcast Net"}
local VAL_BITS = {}

flyweightproto = Proto("flyweight","Flyweight Protocol")
fwfields = flyweightproto.fields
fwfields.f_packet_header = ProtoField.uint16("flyweight.header", "Header", base.HEX)
fwfields.f_src_addr = ProtoField.uint8("flyweight.source", "Src", base.HEX)
fwfields.f_dst_addr = ProtoField.uint8("flyweight.dest", "Dst", base.HEX)
fwfields.f_ackid = ProtoField.uint16("flyweight.ackid", "AckID", base.DEC)

fwfields.f_packet_header_ver = ProtoField.uint8("flyweight.version" , "Packet Version", base.DEC, VER_BITS, 0xc0)
fwfields.f_packet_header_type = ProtoField.uint8("flyweight.type" , "Packet Type", base.DEC,TYPE_BITS, 0x20)
fwfields.f_packet_header_reserved = ProtoField.uint8("flyweight.reserved" , "Reserved", base.DEC,VAL_BITS, 0x10)
fwfields.f_packet_header_network = ProtoField.uint8("flyweight.network" , "Network ID", base.DEC,NET_BITS, 0x0f)

-- Flyweight dissector function
function flyweightproto.dissector(tvb, pkt, root)
  -- validate packet length is adequate, otherwise quit
  if tvb:len() == 0 then return end
  pkt.cols.protocol = flyweightproto.name
  
  local src_addr = string.format("%02X", tvb:range(3,1):uint())
  local dst_addr = string.format("%02X", tvb:range(4,1):uint())
  local ackid = tvb:range(5,1):uint()
  pkt.cols.info = "Src: "..src_addr.." Dst: "..dst_addr.." AckID: "..ackid  

  -- create subtree for Flyweight
  flyweight_subtree = root:add(flyweightproto, tvb(0))
  flyweight_subtree:add_packet_field(fwfields.f_packet_header_ver, tvb:range(2,1), ENC_BIG_ENDIAN)
  flyweight_subtree:add_packet_field(fwfields.f_packet_header_type, tvb:range(2,1), ENC_BIG_ENDIAN)
  flyweight_subtree:add_packet_field(fwfields.f_packet_header_reserved, tvb:range(2,1), ENC_BIG_ENDIAN)
  flyweight_subtree:add_packet_field(fwfields.f_packet_header_network, tvb:range(2,1), ENC_BIG_ENDIAN)
  flyweight_subtree:add_packet_field(fwfields.f_src_addr, tvb:range(3,1), ENC_BIG_ENDIAN)
  flyweight_subtree:add_packet_field(fwfields.f_dst_addr, tvb:range(4,1), ENC_BIG_ENDIAN)
  flyweight_subtree:add_packet_field(fwfields.f_ackid, tvb:range(5,1), ENC_BIG_ENDIAN)
end

-- Initialization routine
function flyweightproto.init()
end

----------------------------------------------------------
-- register a  dissector for ZEP
table = DissectorTable.get("udp.port")
zep_dissector = table:get_dissector(17754)
table:add(17754, zepproto)

-- register a  dissector for Flyweight
table = DissectorTable.get("wtap_encap")
wpan_dissector = table:get_dissector(104)
wpan_nofcs_dissector = table:get_dissector(127)
table:add(0, flyweightproto)
table:add(127, flyweightproto)
