package com.ecommerce.project.service;

import com.ecommerce.project.exceptions.InvalidLengthException;
import com.ecommerce.project.exceptions.ResourceNotFoundException;
import com.ecommerce.project.model.Address;
import com.ecommerce.project.model.Category;
import com.ecommerce.project.model.User;
import com.ecommerce.project.payload.AddressDTO;
import com.ecommerce.project.payload.CategoryDTO;
import com.ecommerce.project.repositories.AddressRepository;
import com.ecommerce.project.repositories.UserRepository;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service class responsible for handling operations related to Address entities.
 * It includes functionality to create, update, retrieve, and validate addresses
 * associated with users in the system.
 */
@Service
public class AddressServiceImpl implements AddressService {

    @Autowired
    private ModelMapper modelMapper;

    @Autowired
    private AddressRepository addressRepository;

    @Autowired
    private UserRepository userRepository;

    /**
     * Creates a new address associated with a specific user.
     *
     * @param addressDTO the address data transfer object
     * @param user       the user to associate the address with
     * @return the saved address as a DTO
     */
    @Override
    public AddressDTO createAddress(AddressDTO addressDTO, User user) {
        validateAddressFields(addressDTO);

        Address address = modelMapper.map(addressDTO, Address.class);
        address.setUser(user);
        user.getAddresses().add(address);

        Address savedAddress = addressRepository.save(address);
        return modelMapper.map(savedAddress, AddressDTO.class);
    }

    /**
     * Retrieves all addresses in the system.
     *
     * @return list of all address DTOs
     */
    @Override
    public List<AddressDTO> getAddresses() {
        List<Address> addresses = addressRepository.findAll();
        if (addresses.isEmpty()) {
            throw new ResourceNotFoundException("No addresses available.");
        }

        return addresses.stream()
                .map(address -> modelMapper.map(address, AddressDTO.class))
                .toList();
    }

    /**
     * Retrieves a specific address by its ID.
     *
     * @param addressId the ID of the address
     * @return the address DTO
     */
    @Override
    public AddressDTO getAddressById(Long addressId) {
        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new ResourceNotFoundException("Addresses", "addressId", addressId));

        return modelMapper.map(address, AddressDTO.class);
    }

    /**
     * Retrieves all addresses associated with a specific user.
     *
     * @param user the user whose addresses to retrieve
     * @return list of address DTOs
     */
    @Override
    public List<AddressDTO> getUserAddresses(User user) {
        List<Address> addresses = user.getAddresses();
        if (addresses.isEmpty()) {
            throw new ResourceNotFoundException("No addresses available.");
        }

        return addresses.stream()
                .map(address -> modelMapper.map(address, AddressDTO.class))
                .toList();
    }

    /**
     * Updates an existing address by ID using the provided data.
     *
     * @param addressId  the ID of the address to update
     * @param addressDTO the new address data
     * @return the updated address as a DTO
     */
    @Override
    public AddressDTO updateAddress(Long addressId, AddressDTO addressDTO) {
        Address addressFromDb = addressRepository.findById(addressId)
                .orElseThrow(() -> new ResourceNotFoundException("Address", "addressId", addressId));

        validateAddressFields(addressDTO);
        updateAddressFields(addressFromDb, addressDTO);

        Address updatedAddress = addressRepository.save(addressFromDb);

        User user = addressFromDb.getUser();
        user.getAddresses().removeIf(address -> address.getAddressId().equals(addressId));
        user.getAddresses().add(updatedAddress);
        userRepository.save(user);

        return modelMapper.map(updatedAddress, AddressDTO.class);
    }

    @Override
    public AddressDTO deleteAddress(Long addressId) {
        Address addressFromDb = addressRepository.findById(addressId)
                .orElseThrow(() -> new ResourceNotFoundException("Address","addressId",addressId));

        User user = addressFromDb.getUser();
        user.getAddresses().removeIf(address -> address.getAddressId().equals(addressId));
        userRepository.save(user);

        addressRepository.delete(addressFromDb);

        return modelMapper.map(addressFromDb, AddressDTO.class);
    }

    /**
     * Validates that a given field is not null, not empty, and meets a minimum length.
     *
     * @param value        the value to validate
     * @param minLength    the minimum required length
     * @param propertyName the name of the property for error messages
     */
    private void validateField(String value, int minLength, String propertyName) {
        if (value == null || value.trim().isEmpty()) {
            throw new InvalidLengthException("The value of '" + propertyName + "' cannot be empty.");
        }

        if (value.trim().length() < minLength) {
            throw new InvalidLengthException("The value of '" + propertyName + "' must be at least " + minLength + " characters.");
        }
    }

    /**
     * Validates all fields of an AddressDTO using their respective minimum lengths.
     *
     * @param addressDTO the address DTO to validate
     */
    private void validateAddressFields(AddressDTO addressDTO) {
        validateField(addressDTO.getCountry(), 2, "country");
        validateField(addressDTO.getCity(), 4, "city");
        validateField(addressDTO.getStreet(), 5, "street");
        validateField(addressDTO.getPinCode(), 5, "pin code");
        validateField(addressDTO.getBuildingName(), 5, "building name");
        validateField(addressDTO.getState(), 2, "state");
    }

    /**
     * Updates the fields of an address entity from the given DTO.
     *
     * @param addressFromDb the address entity to update
     * @param addressDTO    the DTO containing new values
     */
    private void updateAddressFields(Address addressFromDb, AddressDTO addressDTO) {
        addressFromDb.setCountry(addressDTO.getCountry());
        addressFromDb.setCity(addressDTO.getCity());
        addressFromDb.setStreet(addressDTO.getStreet());
        addressFromDb.setPinCode(addressDTO.getPinCode());
        addressFromDb.setBuildingName(addressDTO.getBuildingName());
        addressFromDb.setState(addressDTO.getState());
    }
}