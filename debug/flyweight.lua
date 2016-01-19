-- Codeshelf/Flyweight dissectors
-- FLyweight packets are 802.15.4 packets with an FCF of 0x7eff (Freescale SMAC)
-- They may come to Wireshark in two formats: encapsulated in ZEP packets or in raw 802.15.4 (wpan, wpan_nofcs) packets.
-- For this reason we have to intercept both kinds of encapsulation and chain the dissectors together to get the parsing done.
-- This allows us to digest the 802.15.4 packets in any format we want.

-- Add the following lines (uncommented) to the init.lua file in the Wireshark application

-- FLYWEIGHT_SCRIPT_PATH="/Users/jeffw/git/Codeshelf/debug/"
-- dofile(FLYWEIGHT_SCRIPT_PATH.."flyweight.lua")


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
local NET_IDS = {[1] = "1", [2] = "2", [3] = "3", [15] = "Broadcast Net"}
local GROUP_BITS = {[0] = "NetMgmt", [1] = "Assoc", [2] = "Info", [3] = "Control"}
local VAL_BITS = {}
local NET_CMDS = {[0] = "NetSetup", [1] = "NetCheck", [2] = "NetIntfTest" }
local NET_CHECK_TYPES = { [1] = "Req", [2] = "Resp"}

local ASSOC_CMDS = {[0] = "AssocReq", [1] = "AssocResp", [2] = "AssocCheck", [3] = "AssocACK" }
local ASSOC_RESTART_CAUSES = {[0] = "Unknown", [1] = "User Restart", [2] = "Hard Fault", [3] = "WatchDog Timeout", [4] = "Response Timeout", [5] = "Tx Buffer Full", [6] = "Rx Buffer Full", [7] = "SMAC Error", [9] = "Other", [10] = "Power On"}

local CONTROL_CMDS = {[0] = "Scan", [1] = "Message", [2] = "LED", [3] = "Set PosCtl", [4] = "Clr PosCtl", [5] = "Button", [6] = "Message Single", [7] = "Clear Display", [10] = "CMD Ack" }
local EFFECTS = {[0] = "Solid", [1] = "Flash", [2] = "Error", [3] = "Motel" }

flyweightproto = Proto("flyweight","Flyweight Protocol")
fwfields = flyweightproto.fields
fwfields.f_packet_header = ProtoField.uint16("flyweight.header", "Header", base.HEX)
fwfields.f_src_addr = ProtoField.uint16("flyweight.source", "Src", base.HEX)
fwfields.f_dst_addr = ProtoField.uint16("flyweight.dest", "Dst", base.HEX)
fwfields.f_ackid = ProtoField.uint16("flyweight.ackid", "AckID", base.DEC)

fwfields.f_packet_header_ver = ProtoField.uint8("flyweight.version" , "Packet Version", base.DEC, VER_BITS, 0xc0)
fwfields.f_packet_header_type = ProtoField.uint8("flyweight.type" , "Packet Type", base.DEC,TYPE_BITS, 0x20)
fwfields.f_packet_header_reserved = ProtoField.uint8("flyweight.reserved" , "Reserved", base.DEC,VAL_BITS, 0x10)
fwfields.f_packet_header_network = ProtoField.uint8("flyweight.network" , "Network ID", base.DEC,NET_IDS, 0x0f)

fwfields.f_command_group = ProtoField.uint8("flyweight.cmd.group" , "Command Group", base.DEC, GROUP_BITS, 0xf0)
fwfields.f_command_ep = ProtoField.uint8("flyweight.cmd.ep" , "Endpoint", base.DEC, VAL_BITS, 0x0f)

fwfields.f_netmgmt_id = ProtoField.uint8("flyweight.netmgmt.id" , "Type", base.DEC, NET_CMDS)
fwfields.f_netmgmt_type = ProtoField.uint8("flyweight.netmgt.type" , "Type", base.DEC, NET_CHECK_TYPES)
fwfields.f_netmgmt_netid = ProtoField.uint8("flyweight.netmgt.netid" , "NetId", base.DEC)
fwfields.f_netmgmt_guid = ProtoField.string("flyweight.netmgt.guid" , "GUID", ftypes.STRING)
fwfields.f_netmgmt_channel = ProtoField.uint8("flyweight.netmgt.channel" , "Channel", base.DEC)
fwfields.f_netmgmt_energy = ProtoField.uint8("flyweight.netmgt.energy" , "Energy", base.DEC)
fwfields.f_netmgmt_lqi = ProtoField.uint8("flyweight.netmgt.lqi" , "LQI", base.DEC)

fwfields.f_assoc_cmd = ProtoField.uint8("flyweight.assoc.cmd" , "Type", base.DEC, ASSOC_CMDS)
fwfields.f_assoc_guid = ProtoField.string("flyweight.assoc.guid" , "GUID", ftypes.STRING)
fwfields.f_assoc_resp_addr = ProtoField.uint8("flyweight.assoc.resp.addr" , "Assigned Addr", base.DEC)
fwfields.f_assoc_resp_netid = ProtoField.uint8("flyweight.assoc.resp.netid" , "Assigned NetId", base.DEC, NET_IDS, 0x0f)
fwfields.f_assoc_resp_sleep_sec = ProtoField.uint8("flyweight.assoc.resp.sleep" , "Sleep Sec", base.DEC)

fwfields.f_assoc_batt_lvl = ProtoField.uint8("flyweight.assoc.chck.battlvl" , "Batt Lvl", base.DEC)
fwfields.f_assoc_restart_cause = ProtoField.uint8("flyweight.assoc.chck.restartcause" , "Restart Cause", base.DEC, ASSOC_RESTART_CAUSES)
fwfields.f_assoc_restart_data = ProtoField.uint8("flyweight.assoc.chck.restartdata" , "Restart Data", base.DEC)
fwfields.f_assoc_restart_pc = ProtoField.uint8("flyweight.assoc.chck.restartpc" , "Restart PC", base.DEC)

fwfields.f_info_cmd = ProtoField.uint8("flyweight.info.cmd" , "Type", base.DEC)

fwfields.f_control_cmd = ProtoField.uint8("flyweight.control.cmd" , "Type", base.DEC, CONTROL_CMDS)
fwfields.f_control_led_chan = ProtoField.uint8("flyweight.led.chan" , "Channel", base.DEC)
fwfields.f_control_led_effect = ProtoField.uint8("flyweight.led.effect" , "Effect", base.DEC, EFFECTS)
fwfields.f_control_led_samples = ProtoField.uint8("flyweight.led.samples" , "Samples", base.DEC)
fwfields.f_control_led_pos = ProtoField.int8("flyweight.led.pos" , "Pos", base.DEC)
fwfields.f_control_led_red = ProtoField.uint8("flyweight.led.red" , "Red", base.DEC)
fwfields.f_control_led_green = ProtoField.uint8("flyweight.led.green" , "Green", base.DEC)
fwfields.f_control_led_blue = ProtoField.uint8("flyweight.led.blue" , "Blue", base.DEC)

fwfields.f_control_pos_samples = ProtoField.uint8("flyweight.pos.samples" , "Samples", base.DEC)
fwfields.f_control_pos_num = ProtoField.uint8("flyweight.pos.num" , "Pos Num", base.DEC)
fwfields.f_control_pos_reqval = ProtoField.uint8("flyweight.pos.reqval" , "ReqVal", base.DEC)
fwfields.f_control_pos_minval = ProtoField.uint8("flyweight.pos.minval" , "MinVal", base.DEC)
fwfields.f_control_pos_maxval = ProtoField.uint8("flyweight.pos.maxval" , "MaxVal", base.DEC)
fwfields.f_control_pos_pwmfreq = ProtoField.uint8("flyweight.pos.freq" , "Freq", base.DEC)
fwfields.f_control_pos_pwmduty = ProtoField.uint8("flyweight.pos.duty" , "Duty", base.DEC)

fwfields.f_control_ack_num = ProtoField.uint8("flyweight.ack.acknum" , "AckNum", base.DEC)
fwfields.f_control_ack_lqi = ProtoField.uint8("flyweight.ack.lqi" , "AckLQI" , base.DEC)

fwfields.f_msgone_msg = ProtoField.string("flyweight.msgone.msg" , "Message", ftypes.STRING)
fwfields.f_msgone_font = ProtoField.uint8("flyweight.msgone.font" , "Font", base.DEC)
fwfields.f_msgone_x = ProtoField.uint8("flyweight.msgone.x" , "X", base.DEC)
fwfields.f_msgone_y = ProtoField.uint8("flyweight.msgone.y" , "Y", base.DEC)

local f_control_led_chan_field = Field.new("flyweight.led.chan")
local f_control_led_effect_field = Field.new("flyweight.led.effect")
local f_control_led_samples_field = Field.new("flyweight.led.samples")

local f_control_pos_samples_field = Field.new("flyweight.pos.samples")

local f_assoc_resp_addr = Field.new("flyweight.assoc.resp.addr")
local f_assoc_resp_netid = Field.new("flyweight.assoc.resp.netid")

local addr_map = {}

-- Flyweight dissector function
function flyweightproto.dissector(tvb, pkt, root)
  -- validate packet length is adequate, otherwise quit
  if tvb:len() == 0 then return end
  pkt.cols.protocol = flyweightproto.name
  
  -- create subtree for Flyweight
  flyweight_tree = root:add(flyweightproto, tvb(0))
  flyweight_tree:add_packet_field(fwfields.f_packet_header_ver, tvb:range(2,1), ENC_BIG_ENDIAN)
  
  
  local pkt_ver =  math.floor(tvb:range(2,1):uint() / 64)
  
  -- create subtree for Flyweight v0
  if pkt_ver == 0 then
	  flyweight_tree:add_packet_field(fwfields.f_packet_header_ver, tvb:range(2,1), ENC_BIG_ENDIAN)
	  flyweight_tree:add_packet_field(fwfields.f_packet_header_type, tvb:range(2,1), ENC_BIG_ENDIAN)
	  flyweight_tree:add_packet_field(fwfields.f_packet_header_reserved, tvb:range(2,1), ENC_BIG_ENDIAN)
	  flyweight_tree:add_packet_field(fwfields.f_packet_header_network, tvb:range(2,1), ENC_BIG_ENDIAN)
	  flyweight_tree:add_packet_field(fwfields.f_src_addr, tvb:range(3,1), ENC_BIG_ENDIAN)
	  flyweight_tree:add_packet_field(fwfields.f_dst_addr, tvb:range(4,1), ENC_BIG_ENDIAN)
	  flyweight_tree:add_packet_field(fwfields.f_ackid, tvb:range(5,1), ENC_BIG_ENDIAN)
	
	  flyweight_tree:add_packet_field(fwfields.f_command_group, tvb:range(6,1), ENC_BIG_ENDIAN)
	  flyweight_tree:add_packet_field(fwfields.f_command_ep, tvb:range(6,1), ENC_BIG_ENDIAN)
	  
	  local src_addr = tvb:range(3,1):uint()
	  if src_addr == 0x00 then
	    pkt.cols.src = "gateway"
	  elseif src_addr == 0x0f then
	    pkt.cols.src = "broadcast"
	  elseif addr_map[src_addr] ~= nil then
	    pkt.cols.src = addr_map[src_addr]
	  else
	    pkt.cols.src = src_addr
	  end
	
	  local dst_addr = tvb:range(4,1):uint()
	  if dst_addr == 0x00 then
	    pkt.cols.dst = "gateway"
	  elseif dst_addr == 0xff then
	    pkt.cols.dst = "broadcast"
	  elseif addr_map[dst_addr] ~= nil then
	    pkt.cols.dst = addr_map[dst_addr]
	  else
	    pkt.cols.dst = dst_addr
	  end
	  
	  local ackid = tvb:range(5,1):uint()
	  local packet_type = tvb:range(2,1):bitfield(2,1)
	  if packet_type == 1 then
	    pkt.cols.info= "Ack Packet Id: "..ackid
	  else
	    --local cmd_group = bit32.arshift(bit32.band(tvb:range(6,1):uint(), 0xf0), 4)
	    local cmd_group = math.floor(tvb:range(6,1):uint() / 16)
	    if cmd_group == 0 then
	      netmgmtv1(tvb, pkt, root, flyweight_tree)
	    elseif cmd_group == 1 then
	      assocv1(tvb, pkt, root, flyweight_tree)
	    elseif cmd_group == 2 then
	      infov1(tvb, pkt, root, flyweight_tree)
	    elseif cmd_group == 3 then
	      controlv1(tvb, pkt, root, flyweight_tree)
	    end
	  end
	  
	  if ackid ~= 0 then
	    pkt.cols.info:append(" AckId: "..ackid)
	  end
  
  -- create subtree for Flyweight v1
  elseif pkt_ver == 1 then
	  flyweight_tree:add_packet_field(fwfields.f_packet_header_type, tvb:range(2,1), ENC_BIG_ENDIAN)
	  flyweight_tree:add_packet_field(fwfields.f_packet_header_reserved, tvb:range(2,1), ENC_BIG_ENDIAN)
	  flyweight_tree:add_packet_field(fwfields.f_packet_header_network, tvb:range(2,1), ENC_BIG_ENDIAN)
	  flyweight_tree:add_packet_field(fwfields.f_src_addr, tvb:range(3,2), ENC_BIG_ENDIAN)
	  flyweight_tree:add_packet_field(fwfields.f_dst_addr, tvb:range(5,2), ENC_BIG_ENDIAN)
	  flyweight_tree:add_packet_field(fwfields.f_ackid, tvb:range(7,1), ENC_BIG_ENDIAN)
	
	  flyweight_tree:add_packet_field(fwfields.f_command_group, tvb:range(8,1), ENC_BIG_ENDIAN)
	  flyweight_tree:add_packet_field(fwfields.f_command_ep, tvb:range(8,1), ENC_BIG_ENDIAN)
	  
	  local src_addr = tvb:range(3,2):uint()
	  if src_addr == 0x0000 then
	    pkt.cols.src = "gateway"
	  elseif src_addr == 0xffff then
	    pkt.cols.src = "broadcast"
	  elseif addr_map[src_addr] ~= nil then
	    pkt.cols.src = addr_map[src_addr]
	  else
	    pkt.cols.src = src_addr
	  end
	
	  local dst_addr = tvb:range(5,2):uint()
	  if dst_addr == 0x0000 then
	    pkt.cols.dst = "gateway"
	  elseif dst_addr == 0xffff then
	    pkt.cols.dst = "broadcast"
	  elseif addr_map[dst_addr] ~= nil then
	    pkt.cols.dst = addr_map[dst_addr]
	  else
	    pkt.cols.dst = dst_addr
	  end
	  
	  local ackid = tvb:range(7,1):uint()
	  local packet_type = tvb:range(2,1):bitfield(2,1)
	  if packet_type == 1 then
	    pkt.cols.info= "Ack Packet Id: "..ackid
	  else
	    --local cmd_group = bit32.arshift(bit32.band(tvb:range(8,1):uint(), 0xf0), 4)
	    local cmd_group = math.floor(tvb:range(8,1):uint() / 16)
	    if cmd_group == 0 then
	      netmgmtv2(tvb, pkt, root, flyweight_tree)
	    elseif cmd_group == 1 then
	      assocv2(tvb, pkt, root, flyweight_tree)
	    elseif cmd_group == 2 then
	      infov2(tvb, pkt, root, flyweight_tree)
	    elseif cmd_group == 3 then
	      controlv2(tvb, pkt, root, flyweight_tree)
	    end
	  end
	  
	  if ackid ~= 0 then
	    pkt.cols.info:append(" AckId: "..ackid)
	  end
  end
end

-- Parse netmgmt v1 command
function netmgmtv1(tvb, pkt, root, flyweight_tree)

  local netmgmt_tree = flyweight_tree:add_packet_field(fwfields.f_netmgmt_id, tvb:range(7,1), ENC_BIG_ENDIAN)
  
  local netmgmt_cmd = tvb:range(7,1):uint()
  
  if netmgmt_cmd == 0 then
    netmgmt_tree:add_packet_field(fwfields.f_netmgmt_type, tvb:range(8,1), ENC_BIG_ENDIAN)
    pkt.cols.info = "NetSetup"
  
    local data_dissector = Dissector.get("data")
    data_dissector:call(tvb(9):tvb(), pkt, root)
  elseif netmgmt_cmd == 1 then
    netmgmt_tree:add_packet_field(fwfields.f_netmgmt_type, tvb:range(8,1), ENC_BIG_ENDIAN)
    netmgmt_tree:add_packet_field(fwfields.f_netmgmt_netid, tvb:range(9,1), ENC_BIG_ENDIAN)
    netmgmt_tree:add_packet_field(fwfields.f_netmgmt_guid, tvb:range(10,8), ENC_BIG_ENDIAN)
    netmgmt_tree:add_packet_field(fwfields.f_netmgmt_channel, tvb:range(18,1), ENC_BIG_ENDIAN)
    netmgmt_tree:add_packet_field(fwfields.f_netmgmt_energy, tvb:range(19,1), ENC_BIG_ENDIAN)
    netmgmt_tree:add_packet_field(fwfields.f_netmgmt_lqi, tvb:range(20,1), ENC_BIG_ENDIAN)
    pkt.cols.info = "NetCheck (Beacon)" 

    local data_dissector = Dissector.get("data")
    data_dissector:call(tvb(21):tvb(), pkt, root)
  elseif netmgmt_cmd == 2 then
    netmgmt_tree:add_packet_field(fwfields.f_netmgmt_type, tvb:range(8,1), ENC_BIG_ENDIAN)
    pkt.cols.info = "Net Intf Test"

    local data_dissector = Dissector.get("data")
    data_dissector:call(tvb(9):tvb(), pkt, root)
  end
    
end

-- Parse netmgmt command
function netmgmtv2(tvb, pkt, root, flyweight_tree)

  local netmgmt_tree = flyweight_tree:add_packet_field(fwfields.f_netmgmt_id, tvb:range(9,1), ENC_BIG_ENDIAN)
  
  local netmgmt_cmd = tvb:range(9,1):uint()
  
  if netmgmt_cmd == 0 then
    netmgmt_tree:add_packet_field(fwfields.f_netmgmt_type, tvb:range(10,1), ENC_BIG_ENDIAN)
    pkt.cols.info = "NetSetup"
  
    local data_dissector = Dissector.get("data")
    data_dissector:call(tvb(9):tvb(), pkt, root)
  elseif netmgmt_cmd == 1 then
    netmgmt_tree:add_packet_field(fwfields.f_netmgmt_type, tvb:range(10,1), ENC_BIG_ENDIAN)
    netmgmt_tree:add_packet_field(fwfields.f_netmgmt_netid, tvb:range(11,1), ENC_BIG_ENDIAN)
    netmgmt_tree:add_packet_field(fwfields.f_netmgmt_guid, tvb:range(12,8), ENC_BIG_ENDIAN)
    netmgmt_tree:add_packet_field(fwfields.f_netmgmt_channel, tvb:range(20,1), ENC_BIG_ENDIAN)
    netmgmt_tree:add_packet_field(fwfields.f_netmgmt_energy, tvb:range(21,1), ENC_BIG_ENDIAN)
    netmgmt_tree:add_packet_field(fwfields.f_netmgmt_lqi, tvb:range(22,1), ENC_BIG_ENDIAN)
    pkt.cols.info = "NetCheck (Beacon)" 

    local data_dissector = Dissector.get("data")
    data_dissector:call(tvb(21):tvb(), pkt, root)
  elseif netmgmt_cmd == 2 then
    netmgmt_tree:add_packet_field(fwfields.f_netmgmt_type, tvb:range(10,1), ENC_BIG_ENDIAN)
    pkt.cols.info = "Net Intf Test"

    local data_dissector = Dissector.get("data")
    data_dissector:call(tvb(9):tvb(), pkt, root)
  end
    
end

-- Parse assoc v1 command
function assocv1(tvb, pkt, root, flyweight_tree)

  local assoc_tree = flyweight_tree:add_packet_field(fwfields.f_assoc_cmd, tvb:range(7,1), ENC_BIG_ENDIAN)
  assoc_tree:add_packet_field(fwfields.f_assoc_guid, tvb:range(8,8), ENC_BIG_ENDIAN)
  
  local assoc_cmd = tvb:range(7,1):uint()
  local guid = tvb:range(8,8):string()
  
  if assoc_cmd == 0 then
    pkt.cols.info = "AssocReq".." GUID: "..guid
  elseif assoc_cmd == 1 then
    pkt.cols.info = "AssocResp".." GUID: "..guid
    assoc_tree:add_packet_field(fwfields.f_assoc_resp_addr, tvb:range(16,1), ENC_BIG_ENDIAN)
    pkt.cols.info:append(" assigned addr: "..f_assoc_resp_addr().display)
    addr_map[f_assoc_resp_addr().value] = guid
    assoc_tree:add_packet_field(fwfields.f_assoc_resp_netid, tvb:range(17,1), ENC_BIG_ENDIAN)
    pkt.cols.info:append(" net: "..f_assoc_resp_netid().display)
    assoc_tree:add_packet_field(fwfields.f_assoc_resp_sleep_sec, tvb:range(18,1), ENC_BIG_ENDIAN)
  elseif assoc_cmd == 2 then
    pkt.cols.info = "AssocCheck".." GUID: "..guid
    assoc_tree:add_packet_field(fwfields.f_assoc_batt_lvl, tvb:range(16,1), ENC_BIG_ENDIAN)
    assoc_tree:add_packet_field(fwfields.f_assoc_restart_cause, tvb:range(17,1), ENC_BIG_ENDIAN)
    assoc_tree:add_packet_field(fwfields.f_assoc_restart_data, tvb:range(18,2), ENC_BIG_ENDIAN)
    assoc_tree:add_packet_field(fwfields.f_assoc_restart_pc, tvb:range(19,1), ENC_BIG_ENDIAN)
  elseif assoc_cmd == 3 then
    pkt.cols.info = "AssocAck".." GUID: "..guid
  end
 
end

-- Parse assoc v2 command
function assocv2(tvb, pkt, root, flyweight_tree)

  local assoc_tree = flyweight_tree:add_packet_field(fwfields.f_assoc_cmd, tvb:range(9,1), ENC_BIG_ENDIAN)
  assoc_tree:add_packet_field(fwfields.f_assoc_guid, tvb:range(10,8), ENC_BIG_ENDIAN)
  
  local assoc_cmd = tvb:range(9,1):uint()
  local guid = tvb:range(10,8):string()
  
  if assoc_cmd == 0 then
    pkt.cols.info = "AssocReq".." GUID: "..guid
  elseif assoc_cmd == 1 then
    pkt.cols.info = "AssocResp".." GUID: "..guid
    assoc_tree:add_packet_field(fwfields.f_assoc_resp_addr, tvb:range(18,1), ENC_BIG_ENDIAN)
    pkt.cols.info:append(" assigned addr: "..f_assoc_resp_addr().display)
    addr_map[f_assoc_resp_addr().value] = guid
    assoc_tree:add_packet_field(fwfields.f_assoc_resp_netid, tvb:range(19,1), ENC_BIG_ENDIAN)
    pkt.cols.info:append(" net: "..f_assoc_resp_netid().display)
    assoc_tree:add_packet_field(fwfields.f_assoc_resp_sleep_sec, tvb:range(20,1), ENC_BIG_ENDIAN)
  elseif assoc_cmd == 2 then
    pkt.cols.info = "AssocCheck".." GUID: "..guid
    assoc_tree:add_packet_field(fwfields.f_assoc_batt_lvl, tvb:range(18,1), ENC_BIG_ENDIAN)
    assoc_tree:add_packet_field(fwfields.f_assoc_restart_cause, tvb:range(19,1), ENC_BIG_ENDIAN)
    assoc_tree:add_packet_field(fwfields.f_assoc_restart_data, tvb:range(20,2), ENC_BIG_ENDIAN)
    assoc_tree:add_packet_field(fwfields.f_assoc_restart_pc, tvb:range(21,1), ENC_BIG_ENDIAN)
  elseif assoc_cmd == 3 then
    pkt.cols.info = "AssocAck".." GUID: "..guid
  end
 
end

-- Parse info v1 command
function infov1(tvb, pkt, root, flyweight_tree)

  local info_tree = flyweight_tree:add_packet_field(fwfields.f_info_cmd, tvb:range(7,1), ENC_BIG_ENDIAN)
  
  local data_dissector = Dissector.get("data")
  data_dissector:call(tvb(9):tvb(), pkt, root)
  
end

-- Parse info v2 command
function infov2(tvb, pkt, root, flyweight_tree)

  local info_tree = flyweight_tree:add_packet_field(fwfields.f_info_cmd, tvb:range(9,1), ENC_BIG_ENDIAN)
  
  local data_dissector = Dissector.get("data")
  data_dissector:call(tvb(9):tvb(), pkt, root)
  
end

-- Parse control v1 command
function controlv1(tvb, pkt, root, flyweight_tree)

  local control_tree = flyweight_tree:add_packet_field(fwfields.f_control_cmd, tvb:range(7,1), ENC_BIG_ENDIAN)
   
  local control_cmd = tvb:range(7,1):uint()
  
  if control_cmd == 0 then
    pkt.cols.info = "Scan"
    local data_dissector = Dissector.get("data")
    data_dissector:call(tvb(8):tvb(), pkt, root)
  elseif control_cmd == 1 then
    pkt.cols.info = "Message"
    local data_dissector = Dissector.get("data")
    data_dissector:call(tvb(8):tvb(), pkt, root)
  elseif control_cmd == 2 then
    pkt.cols.info = "LED"
    control_tree:add_packet_field(fwfields.f_control_led_chan, tvb:range(8,1), ENC_BIG_ENDIAN)
    pkt.cols.info:append(" chan: "..f_control_led_chan_field().display)
    control_tree:add_packet_field(fwfields.f_control_led_effect, tvb:range(9,1), ENC_BIG_ENDIAN)
    pkt.cols.info:append(" effect: "..f_control_led_effect_field().display)
    local sample_tree = control_tree:add_packet_field(fwfields.f_control_led_samples, tvb:range(10,1), ENC_BIG_ENDIAN)
    pkt.cols.info:append(" count: "..f_control_led_samples_field().display)
    local samples = tvb:range(10,1):uint()
    for sample = 0, samples - 1, 1 do
      local offset = sample * 5
      sample_tree:add_packet_field(fwfields.f_control_led_pos, tvb:range(offset + 11,2), ENC_BIG_ENDIAN)
      sample_tree:add_packet_field(fwfields.f_control_led_red, tvb:range(offset + 13,1), ENC_BIG_ENDIAN)
      sample_tree:add_packet_field(fwfields.f_control_led_green, tvb:range(offset + 14,1), ENC_BIG_ENDIAN)
      sample_tree:add_packet_field(fwfields.f_control_led_blue, tvb:range(offset + 15,1), ENC_BIG_ENDIAN)
    end
  elseif control_cmd == 3 then
    pkt.cols.info = "Set Pos Controller"
    local sample_tree = control_tree:add_packet_field(fwfields.f_control_pos_samples, tvb:range(8,1), ENC_BIG_ENDIAN)
    pkt.cols.info:append(" count: "..f_control_pos_samples_field().display)
    local samples = tvb:range(10,1):uint()
    for sample = 0, samples - 1, 1 do
      local offset = sample * 4
      sample_tree:add_packet_field(fwfields.f_control_pos_num, tvb:range(offset + 9,1), ENC_BIG_ENDIAN)
      sample_tree:add_packet_field(fwfields.f_control_pos_reqval, tvb:range(offset + 10,1), ENC_BIG_ENDIAN)
      sample_tree:add_packet_field(fwfields.f_control_pos_minval, tvb:range(offset + 11,1), ENC_BIG_ENDIAN)
      sample_tree:add_packet_field(fwfields.f_control_pos_maxval, tvb:range(offset + 12,1), ENC_BIG_ENDIAN)
      sample_tree:add_packet_field(fwfields.f_control_pos_pwmfreq, tvb:range(offset + 13,1), ENC_BIG_ENDIAN)
      sample_tree:add_packet_field(fwfields.f_control_pos_pwmduty, tvb:range(offset + 14,1), ENC_BIG_ENDIAN)
    end
  elseif control_cmd == 4 then
    pkt.cols.info = "Clr Pos Controller"
    local data_dissector = Dissector.get("data")
    data_dissector:call(tvb(8):tvb(), pkt, root)
  elseif control_cmd == 5 then
    pkt.cols.info = "Button"
    local data_dissector = Dissector.get("data")
    data_dissector:call(tvb(8):tvb(), pkt, root)
  elseif control_cmd == 6 then
    pkt.cols.info = "Message Single"
    control_tree:add_packet_field(fwfields.f_msgone_font, tvb:range(8, 1), ENC_BIG_ENDIAN)
    control_tree:add_packet_field(fwfields.f_msgone_x, tvb:range(9, 2), ENC_BIG_ENDIAN)
    control_tree:add_packet_field(fwfields.f_msgone_y, tvb:range(11, 2), ENC_BIG_ENDIAN)
    local message_len = tvb:range(13,1):uint()
    control_tree:add_packet_field(fwfields.f_msgone_msg, tvb:range(14, message_len), ENC_BIG_ENDIAN)
    local data_dissector = Dissector.get("data")
   elseif control_cmd == 7 then
    pkt.cols.info = "Clear Display"
    local data_dissector = Dissector.get("data")
    data_dissector:call(tvb(8):tvb(), pkt, root)
   elseif control_cmd == 10 then
   	local ack_id = tvb:range(8,1):uint()
   	pkt.cols.info = "CMD Ack "..ack_id
   	control_tree:add_packet_field(fwfields.f_control_ack_num, tvb:range(8, 1), ENC_BIG_ENDIAN)
    control_tree:add_packet_field(fwfields.f_control_ack_lqi, tvb:range(9, 1), ENC_BIG_ENDIAN)
  end

end


-- Parse control v2 command
function controlv2(tvb, pkt, root, flyweight_tree)

  local control_tree = flyweight_tree:add_packet_field(fwfields.f_control_cmd, tvb:range(9,1), ENC_BIG_ENDIAN)
   
  local control_cmd = tvb:range(9,1):uint()
  
  if control_cmd == 0 then
    pkt.cols.info = "Scan"
    local data_dissector = Dissector.get("data")
    data_dissector:call(tvb(8):tvb(), pkt, root)
  elseif control_cmd == 1 then
    pkt.cols.info = "Message"
    local data_dissector = Dissector.get("data")
    data_dissector:call(tvb(8):tvb(), pkt, root)
  elseif control_cmd == 2 then
    pkt.cols.info = "LED"
    control_tree:add_packet_field(fwfields.f_control_led_chan, tvb:range(8,1), ENC_BIG_ENDIAN)
    pkt.cols.info:append(" chan: "..f_control_led_chan_field().display)
    control_tree:add_packet_field(fwfields.f_control_led_effect, tvb:range(9,1), ENC_BIG_ENDIAN)
    pkt.cols.info:append(" effect: "..f_control_led_effect_field().display)
    local sample_tree = control_tree:add_packet_field(fwfields.f_control_led_samples, tvb:range(10,1), ENC_BIG_ENDIAN)
    pkt.cols.info:append(" count: "..f_control_led_samples_field().display)
    local samples = tvb:range(10,1):uint()
    for sample = 0, samples - 1, 1 do
      local offset = sample * 5
      sample_tree:add_packet_field(fwfields.f_control_led_pos, tvb:range(offset + 11,2), ENC_BIG_ENDIAN)
      sample_tree:add_packet_field(fwfields.f_control_led_red, tvb:range(offset + 13,1), ENC_BIG_ENDIAN)
      sample_tree:add_packet_field(fwfields.f_control_led_green, tvb:range(offset + 14,1), ENC_BIG_ENDIAN)
      sample_tree:add_packet_field(fwfields.f_control_led_blue, tvb:range(offset + 15,1), ENC_BIG_ENDIAN)
    end
  elseif control_cmd == 3 then
    pkt.cols.info = "Set Pos Controller"
    local sample_tree = control_tree:add_packet_field(fwfields.f_control_pos_samples, tvb:range(8,1), ENC_BIG_ENDIAN)
    pkt.cols.info:append(" count: "..f_control_pos_samples_field().display)
    local samples = tvb:range(10,1):uint()
    for sample = 0, samples - 1, 1 do
      local offset = sample * 4
      sample_tree:add_packet_field(fwfields.f_control_pos_num, tvb:range(offset + 9,1), ENC_BIG_ENDIAN)
      sample_tree:add_packet_field(fwfields.f_control_pos_reqval, tvb:range(offset + 10,1), ENC_BIG_ENDIAN)
      sample_tree:add_packet_field(fwfields.f_control_pos_minval, tvb:range(offset + 11,1), ENC_BIG_ENDIAN)
      sample_tree:add_packet_field(fwfields.f_control_pos_maxval, tvb:range(offset + 12,1), ENC_BIG_ENDIAN)
      sample_tree:add_packet_field(fwfields.f_control_pos_pwmfreq, tvb:range(offset + 13,1), ENC_BIG_ENDIAN)
      sample_tree:add_packet_field(fwfields.f_control_pos_pwmduty, tvb:range(offset + 14,1), ENC_BIG_ENDIAN)
    end
  elseif control_cmd == 4 then
    pkt.cols.info = "Clr Pos Controller"
    local data_dissector = Dissector.get("data")
    data_dissector:call(tvb(8):tvb(), pkt, root)
  elseif control_cmd == 5 then
    pkt.cols.info = "Button"
    local data_dissector = Dissector.get("data")
    data_dissector:call(tvb(8):tvb(), pkt, root)
  elseif control_cmd == 6 then
    pkt.cols.info = "Message Single"
    control_tree:add_packet_field(fwfields.f_msgone_font, tvb:range(8, 1), ENC_BIG_ENDIAN)
    control_tree:add_packet_field(fwfields.f_msgone_x, tvb:range(9, 2), ENC_BIG_ENDIAN)
    control_tree:add_packet_field(fwfields.f_msgone_y, tvb:range(11, 2), ENC_BIG_ENDIAN)
    local message_len = tvb:range(13,1):uint()
    control_tree:add_packet_field(fwfields.f_msgone_msg, tvb:range(14, message_len), ENC_BIG_ENDIAN)
    local data_dissector = Dissector.get("data")
   elseif control_cmd == 7 then
    pkt.cols.info = "Clear Display"
    local data_dissector = Dissector.get("data")
    data_dissector:call(tvb(8):tvb(), pkt, root)
   elseif control_cmd == 10 then
   	local ack_id = tvb:range(8,1):uint()
   	pkt.cols.info = "CMD Ack "..ack_id
   	control_tree:add_packet_field(fwfields.f_control_ack_num, tvb:range(8, 1), ENC_BIG_ENDIAN)
    control_tree:add_packet_field(fwfields.f_control_ack_lqi, tvb:range(9, 1), ENC_BIG_ENDIAN)
  end

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
