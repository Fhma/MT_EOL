require "csv"
require "set"

#modules = ["ECore2GMF"]
modules = ["ECore2GMF","EcoreHelper","ECoreUtil","Formatting","ShortestPath"]

file1 = File.read("#{modules[0]}.csv")
file2 = File.read("#{modules[1]}.csv")
file3 = File.read("#{modules[2]}.csv")
file4 = File.read("#{modules[3]}.csv")
file5 = File.read("#{modules[4]}.csv")

csv_file1 = CSV.parse(file1, :headers => true)
csv_file2 = CSV.parse(file2, :headers => true)
csv_file3 = CSV.parse(file3, :headers => true)
csv_file4 = CSV.parse(file4, :headers => true)
csv_file5 = CSV.parse(file5, :headers => true)

operators_1 =  Hash.new
operators_2 =  Hash.new
operators_3 =  Hash.new
operators_4 =  Hash.new
operators_5 =  Hash.new

operators_names = []

csv_file1.each do |f1|
	entry = {"Valid"=>f1["Valid"], "Invalid"=>f1["Invalid"]}
	operators_1.store(f1["Mutation Operator"], entry)
	operators_names.push(f1["Mutation Operator"]) if (not operators_names.include? f1["Mutation Operator"])
end

csv_file2.each do |f2|
	entry = {"Valid"=>f2["Valid"], "Invalid"=>f2["Invalid"]}
	operators_2.store(f2["Mutation Operator"], entry)
	operators_names.push(f2["Mutation Operator"]) if (not operators_names.include? f2["Mutation Operator"])
end

csv_file3.each do |f3|
	entry = {"Valid"=>f3["Valid"], "Invalid"=>f3["Invalid"]}
	operators_3.store(f3["Mutation Operator"], entry)
	operators_names.push(f3["Mutation Operator"]) if (not operators_names.include? f3["Mutation Operator"])
end

csv_file4.each do |f4|
	entry = {"Valid"=>f4["Valid"], "Invalid"=>f4["Invalid"]}
	operators_4.store(f4["Mutation Operator"], entry)
	operators_names.push(f4["Mutation Operator"]) if (not operators_names.include? f4["Mutation Operator"])
end

csv_file5.each do |f5|
	entry = {"Valid"=>f5["Valid"], "Invalid"=>f5["Invalid"]}
	operators_5.store(f5["Mutation Operator"], entry)
	operators_names.push(f5["Mutation Operator"]) if (not operators_names.include? f5["Mutation Operator"])
end

CSV.open("all_operators.csv", "wb") do |csv|
	csv << ["Mutation Operator", "Valid","Invalid"]
	operators_names.each do |op|

		entry1 = operators_1.fetch(op) if operators_1.key?(op)
		entry2 = operators_2.fetch(op) if operators_2.key?(op)
		entry3 = operators_3.fetch(op) if operators_3.key?(op)
		entry4 = operators_4.fetch(op) if operators_4.key?(op)
		entry5 = operators_5.fetch(op) if operators_5.key?(op)

		valid,invalid = 0,0,0,0,0,0

		if(entry1 != nil)
			valid = valid + entry1["Valid"].to_i
			invalid = invalid + entry1["Invalid"].to_i
		end

		if(entry2 != nil)
			valid = valid + entry2["Valid"].to_i
			invalid = invalid + entry2["Invalid"].to_i
		end

		if(entry3 != nil)
			valid = valid + entry3["Valid"].to_i
			invalid = invalid + entry3["Invalid"].to_i	
		end

		if(entry4 != nil)
			valid = valid + entry4["Valid"].to_i
			invalid = invalid + entry4["Invalid"].to_i
		end

		if(entry5 != nil)
			valid = valid + entry5["Valid"].to_i
			invalid = invalid + entry5["Invalid"].to_i
		end
		csv << [op,valid,invalid]
	end
end
