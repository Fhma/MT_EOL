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
	entry = {"Gen."=>f1["Gen."], "Trivial"=>f1["Trivial"], "Killed"=>f1["Killed"], "Live"=>f1["Live"], "Equiv."=>f1["Equiv."], "Invalid"=>f1["Invalid"]}
	operators_1.store(f1["Mutation Operator"], entry)
	operators_names.push(f1["Mutation Operator"]) if (not operators_names.include? f1["Mutation Operator"])
end

csv_file2.each do |f2|
	entry = {"Gen."=>f2["Gen."], "Trivial"=>f2["Trivial"], "Killed"=>f2["Killed"], "Live"=>f2["Live"], "Equiv."=>f2["Equiv."], "Invalid"=>f2["Invalid"]}
	operators_2.store(f2["Mutation Operator"], entry)
	operators_names.push(f2["Mutation Operator"]) if (not operators_names.include? f2["Mutation Operator"])
end

csv_file3.each do |f3|
	entry = {"Gen."=>f3["Gen."], "Trivial"=>f3["Trivial"], "Killed"=>f3["Killed"], "Live"=>f3["Live"], "Equiv."=>f3["Equiv."], "Invalid"=>f3["Invalid"]}
	operators_3.store(f3["Mutation Operator"], entry)
	operators_names.push(f3["Mutation Operator"]) if (not operators_names.include? f3["Mutation Operator"])
end

csv_file4.each do |f4|
	entry = {"Gen."=>f4["Gen."], "Trivial"=>f4["Trivial"], "Killed"=>f4["Killed"], "Live"=>f4["Live"], "Equiv."=>f4["Equiv."], "Invalid"=>f4["Invalid"]}
	operators_4.store(f4["Mutation Operator"], entry)
	operators_names.push(f4["Mutation Operator"]) if (not operators_names.include? f4["Mutation Operator"])
end

csv_file5.each do |f5|
	entry = {"Gen."=>f5["Gen."], "Trivial"=>f5["Trivial"], "Killed"=>f5["Killed"], "Live"=>f5["Live"], "Equiv."=>f5["Equiv."], "Invalid"=>f5["Invalid"]}
	operators_5.store(f5["Mutation Operator"], entry)
	operators_names.push(f5["Mutation Operator"]) if (not operators_names.include? f5["Mutation Operator"])
end

CSV.open("all_operators.csv", "wb") do |csv|
	csv << ["Mutation Operator", "Gen.","Trivial", "Killed", "Live", "Equiv.", "Invalid"]
	operators_names.each do |op|

		entry1 = operators_1.fetch(op) if operators_1.key?(op)
		entry2 = operators_2.fetch(op) if operators_2.key?(op)
		entry3 = operators_3.fetch(op) if operators_3.key?(op)
		entry4 = operators_4.fetch(op) if operators_4.key?(op)
		entry5 = operators_5.fetch(op) if operators_5.key?(op)

		gen,triv,kill,live,equiv,inv = 0,0,0,0,0,0

		if(entry1 != nil)
			gen = gen + entry1["Gen."].to_i
			triv = triv + entry1["Trivial"].to_i
			kill = kill + entry1["Killed"].to_i
			live = live + entry1["Live"].to_i
			equiv = equiv + entry1["Equiv."].to_i
			inv = inv + entry1["Invalid"].to_i
		end

		if(entry2 != nil)
			gen = gen + entry2["Gen."].to_i
			triv = triv + entry2["Trivial"].to_i
			kill = kill + entry2["Killed"].to_i
			live = live + entry2["Live"].to_i
			equiv = equiv + entry2["Equiv."].to_i
			inv = inv + entry2["Invalid"].to_i
		end

		if(entry3 != nil)
			gen = gen + entry3["Gen."].to_i
			triv = triv + entry3["Trivial"].to_i
			kill = kill + entry3["Killed"].to_i
			live = live + entry3["Live"].to_i
			equiv = equiv + entry3["Equiv."].to_i
			inv = inv + entry3["Invalid"].to_i
		end

		if(entry4 != nil)
			gen = gen + entry4["Gen."].to_i
			triv = triv + entry4["Trivial"].to_i
			kill = kill + entry4["Killed"].to_i
			live = live + entry4["Live"].to_i
			equiv = equiv + entry4["Equiv."].to_i
			inv = inv + entry4["Invalid"].to_i
		end

		if(entry5 != nil)
			gen = gen + entry5["Gen."].to_i
			triv = triv + entry5["Trivial"].to_i
			kill = kill + entry5["Killed"].to_i
			live = live + entry5["Live"].to_i
			equiv = equiv + entry5["Equiv."].to_i
			inv = inv + entry5["Invalid"].to_i
		end
		csv << [op,gen,triv,kill,live,equiv,inv]
	end
end

=begin
CSV.open("all_operators.csv", "wb") do |csv|
	csv << ["Mutation Operator","Gen.","Trivial","Killed","Live","Equiv.","Invalid"]
	csv_file1.each do |f1|
		csv_file2.each do |f2|
			csv_file3.each do |f3|
				csv_file4.each do |f4|
					csv_file5.each do |f5|
						if f1["Mutation Operator"] == f2["Mutation Operator"] && f2["Mutation Operator"] == f3["Mutation Operator"] && f3["Mutation Operator"] == f4["Mutation Operator"] && f4["Mutation Operator"] == f5["Mutation Operator"]
							gen = f1["Gen."].to_i + f2["Gen."].to_i + f3["Gen."].to_i + f4["Gen."].to_i + f5["Gen."].to_i
							triv = f1["Trivial"].to_i + f2["Trivial"].to_i + f3["Trivial"].to_i + f4["Trivial"].to_i + f5["Trivial"].to_i
							kill = f1["Killed"].to_i + f2["Killed"].to_i + f3["Killed"].to_i + f4["Killed"].to_i + f5["Killed"].to_i
							live = f1["Live"].to_i + f2["Live"].to_i + f3["Live"].to_i + f4["Live"].to_i + f5["Live"].to_i
							equiv = f1["Equiv."].to_i + f2["Equiv."].to_i + f3["Equiv."].to_i + f4["Equiv."].to_i + f5["Equiv."].to_i
							inv = f1["Invalid"].to_i + f2["Invalid"].to_i + f3["Invalid"].to_i + f4["Invalid"].to_i + f5["Invalid"].to_i
							entry = [f1["Mutation Operator"],gen,triv,kill,live,equiv,inv]
							csv<<entry
						end
					end
				end
			end
		end
	end
end
=end

